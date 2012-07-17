/*
Kurento Sip User Agent implementation.
Copyright (C) <2011>  <Tikal Technologies>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License version 3
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.kurento.commons.sip.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.media.format.MediaSpec;
import com.kurento.commons.media.format.SessionSpec;
import com.kurento.commons.media.format.conversor.SdpConversor;
import com.kurento.commons.media.format.enums.MediaType;
import com.kurento.commons.media.format.enums.Mode;
import com.kurento.commons.media.format.exceptions.ArgumentNotSetException;
import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManager;
import com.kurento.commons.sip.transaction.CBye;
import com.kurento.commons.sip.transaction.CCancel;
import com.kurento.commons.sip.transaction.CInvite;
import com.kurento.commons.sip.transaction.CTransaction;
import com.kurento.commons.sip.transaction.STransaction;
import com.kurento.ua.commons.Call;
import com.kurento.ua.commons.CallAttributes;
import com.kurento.ua.commons.CallEvent;
import com.kurento.ua.commons.CallEventEnum;
import com.kurento.ua.commons.CallListener;
import com.kurento.ua.commons.ServerInternalErrorException;
import com.kurento.ua.commons.TerminateReason;

public class SipContext implements Call {

	enum ContextState {
		NULL, EARLY, CONFIRMED, TERMINATED
	}

	protected static final Logger log = LoggerFactory
			.getLogger(SipContext.class);

	private SipEndPointImpl localEndPoint;
	private Address remoteParty;

	// We need to keep a context state because dialog state does not allow us to
	// know with precision the state machine. There is a lag between message
	// command and dialog state change causing severe problems wit concurrence.

	private ContextState state = ContextState.NULL;
	private Dialog dialog;
	private NetworkConnection networkConnection;
	private SdpPortManager sdpPortManager;

	private STransaction incomingInitiatingRequest;
	private CTransaction outgoingInitiatingRequest;
	private Boolean request2Terminate = false;

	private CallListener callListener;
	private CallAttributes callAttributes = new CallAttributes();
	private Map<MediaType, Mode> mediaTypesModes;

	// ////////////////////
	//
	// Constructor
	//
	// ////////////////////

	public SipContext(SipEndPointImpl localEndPoint) {
		this(localEndPoint, null);
	}

	public SipContext(SipEndPointImpl localEndPoint, Dialog dialog) {
		this.localEndPoint = localEndPoint;
		this.dialog = dialog;

	}

	// ////////////////////
	//
	// GETTERS
	//
	// ////////////////////

	public Dialog getDialog() {
		return dialog;
	}

	public SipEndPointImpl getEndPoint() {
		return localEndPoint;
	}

	public Address getRemoteParty() {
		return remoteParty;
	}

	public SdpPortManager getSdpPortmanager()
			throws ServerInternalErrorException {
		createSdpPortManager();
		return sdpPortManager;
	}

	public void setCallAttributes(CallAttributes callAttributes) {
		this.callAttributes = callAttributes;
	}

	// ////////////////////
	//
	// SIP CALL INTERFACE
	//
	// ////////////////////

	@Override
	public CallAttributes getAttributes() {
		return callAttributes;
	}

	@Override
	public void accept() throws ServerInternalErrorException {
		// Accept only if there are incoming transactions
		log.debug("Accept Call: " + getCallInfo());

		if (dialog == null) {
			throw new ServerInternalErrorException(
					"Bad accept. Unable to find a dialog for this call");
		}

		if (incomingInitiatingRequest == null) {
			throw new ServerInternalErrorException(
					"Bad accept. There isn't a incoming request to be accepted");
		}

		// Accept: only possible for EARLY contexts
		if (ContextState.EARLY.equals(state)) {
			stateTransition(ContextState.CONFIRMED);
			incomingInitiatingRequest.sendResponse(Response.OK, getLocalSdp());
		} else {
			throw new ServerInternalErrorException(
					"Bad accept. Context state is not EARLY. state=" + state);
		}

	}

	@Override
	public void terminate() throws ServerInternalErrorException {
		terminate(TerminateReason.DECLINE);

	}

	@Override
	public void terminate(TerminateReason code)
			throws ServerInternalErrorException {

		// Label this context to be terminated as soon as possible
		request2Terminate = true;

		// Check valid states where a call can be canceled
		if (state == ContextState.NULL) {
			// State is null until INVITE request is sent.
			// DO NOTHING. Cancel must be sent after invite is sent
			log.debug("Request to terminate outgoing call with no INVITE transaction created yet: " + getCallInfo());

		} else if (ContextState.EARLY.equals(state)
				&& outgoingInitiatingRequest != null) {
			// Hang out an outgoing call after INVITE request is sent and
			// before response is received
			log.debug("Request to terminate pending outgoing call: "+ getCallInfo());
			// Send cancel request
			localCallCancel();

		} else if (ContextState.CONFIRMED.equals(state)) {
			// Terminate request after 200 OK response. ACK might still not
			// being received
			log.debug("Request to terminate established call (ACK might still be pending):" + getCallInfo());
			// Change state before request to avoid concurrent BYE requests from
			// local party
			stateTransition(ContextState.TERMINATED);
			new CBye(this);

		} else if (ContextState.EARLY.equals(state)
				&& incomingInitiatingRequest != null) {
			// TU requested CALL reject
			log.debug("Request to reject incoming call: " + getCallInfo());
			// This code competes with the remote cancel. First one to execute
			// will cause the other to throw an exception avoiding duplicate
			// events
			// Change state before response to avoid concurrent events with
			// remote CANCEL events
			stateTransition(ContextState.TERMINATED);
			log.debug("Request to reject a call with code: " + code);
			if (TerminateReason.BUSY.equals(code)) {
				incomingInitiatingRequest
						.sendResponse(Response.BUSY_HERE, null);
			} else {
				incomingInitiatingRequest.sendResponse(Response.DECLINE, null);
			}
			rejectedCall();
		} else if (ContextState.TERMINATED.equals(state)) {
			log.info("Call already terminated when hangup request,"
					+ dialog.getDialogId() +": " + getCallInfo());
		}

		// Do not accept call to this method
		else {
			throw new ServerInternalErrorException(
					"Bad hangup. Unable to hangup a call ("+ getCallInfo()+ ") with current state: "
							+ state);
		}

	}

	@Override
	public void addListener(CallListener listener) {
		callListener = listener;
	}

	@Override
	public void removeListener(CallListener listener) {
		if (listener == callListener)
			callListener = null;
	}

	@Override
	public NetworkConnection getNetworkConnection() {
		return networkConnection;
	}

	@Override
	public Map<MediaType, Mode> getMediaTypesModes() {
		return mediaTypesModes;
	}

	@Override
	public Boolean isConnected() {
		if (dialog != null && DialogState.CONFIRMED.equals(dialog.getState()))
			return true;
		else
			return false;
	}

	@Override
	public String getRemoteUri() {
		if (remoteParty == null)
			return null;
		return remoteParty.getURI().toString();
	}

	@Override
	public String getRemoteDisplayName() {
		if (remoteParty == null)
			return null;
		return remoteParty.toString();
	}

	@Override
	public String getId() {
		if (dialog != null)
			return dialog.getCallId().toString();
		else
			return "";
	}

	private Map<MediaType, Mode> getModesOfMediaTypes() {
		Map<MediaType, Mode> map = new HashMap<MediaType, Mode>();
		if (sdpPortManager != null) {
			try {
				SessionSpec session = this.sdpPortManager
						.getMediaServerSessionDescription();

				if (session == null)
					return map;

				for (MediaSpec m : session.getMediaSpecs()) {
					// Only it is to check that there is a rtp transport
					try {
						m.getTransport().getRtp();
					} catch (ArgumentNotSetException ex) {
						continue;
					}

					// Check that Mode is Inactive
					if (m.getMode() == Mode.INACTIVE)
						continue;

					Set<MediaType> mediaTypes = m.getTypes();

					if (mediaTypes.size() != 1)
						continue;

					for (MediaType t : mediaTypes) {
						map.put(t, m.getMode());
						break;
					}
				}
			} catch (Exception e) {
				log.error(
						"Unable to retrieve SDP port manager while creating media type map",
						e);
			}
		}
		return map;
	}

	// /////////////////////
	//
	// Sip End Point API
	//
	// //////////////////////

	protected void connect(Address remoteParty)
			throws ServerInternalErrorException {
		this.remoteParty = remoteParty;
		log.info("Request connection from " + localEndPoint.getAddress()
				+ " => To => " + remoteParty);
		new CInvite(this);
		// Do not associate this CInvite to the outgoing call until the INVITE
		// request is sent. This will be done through notification outgoingCall
		// This prevent run conditions when hanging up while SDP is generated
	}

	// ////////////////////
	//
	// Transaction Interface
	//
	// ////////////////////

	// /////// COMMANDS

	// Used by SCancel transaction to notify reception of CANCEL request

	public void remoteCallCancel() throws ServerInternalErrorException {
		log.info("Request call Cancel from remote peer");
		request2Terminate = true;
		if (incomingInitiatingRequest != null
				&& ContextState.EARLY.equals(state)) {
			// Cancel received after SDP offer has been process
			// Send now the response and before 200 OK response has been sent
			// (accept)
			stateTransition(ContextState.TERMINATED);
			incomingInitiatingRequest.sendResponse(Response.REQUEST_TERMINATED,
					null);
			canceledCall();
			// Remove reference to the initiating request
//			incomingInitiatingRequest = null;
		} else {
			// Cancel received before the SDP has been processed. Wait
			// incomingCall event before cancel can be performed
			log.info("Incoming pending request to cancel not yet processed");
		}
	}

	// /////// EVENTS

	// Use by CInvite to notify when the SDP offer has been generated and
	// request sent. This method is called when dialog satate is EARLY
	public void outgoingCall(CTransaction outgoingTransaction) {

		if (outgoingTransaction == null)
			return;

		this.dialog = outgoingTransaction.getClientTransaction().getDialog();
		this.dialog.setApplicationData(this);
		this.outgoingInitiatingRequest = outgoingTransaction;

		stateTransition(ContextState.EARLY);
		if (request2Terminate) {
			// Cancel request received from local party while SDP was generated.
			try {
				localCallCancel();
			} catch (ServerInternalErrorException e) {
				log.warn("Unable to cancel outgoing call", e);
			}
		}
	}

	// Use by SInvite to notify an incoming INVITE request. SDP offer is already
	// process and the SDP answer is ready to be sent
	public void incominCall(STransaction incomingTransaction) {

		if (incomingTransaction == null)
			return;

		// Record remote party
		this.remoteParty = incomingTransaction.getServerTransaction()
				.getDialog().getRemoteParty();
		this.incomingInitiatingRequest = incomingTransaction;

		if (request2Terminate) {
			// This condition can verify when a remote CANCEL request is
			// received before the SDP offer of incoming INVITE is still being
			// processed
			// Force call cancel and do not signal incoming to the controller
			stateTransition(ContextState.TERMINATED);
			try {
				// Change before transition to avoid concurrent conflict with
				// local reject
				incomingTransaction.sendResponse(Response.REQUEST_TERMINATED,
						null);
				canceledCall();
			} catch (ServerInternalErrorException e) {
				log.warn("Unable to terminate call canceled by remote party", e);
				// Controller doesn't know about this call. Do not signall
				// anything
			}
			release();
		} else {
			// Received INVITE request and no terminate request received in
			// between => Transition to EARLY
			stateTransition(ContextState.EARLY);

			// Notify the incoming call to EndPoint controllers and waits for
			// response (accept or reject)
			// Store pending request
			log.info("Incoming call signalled with callId:"
					+ incomingInitiatingRequest.getServerTransaction()
							.getDialog().getCallId());
			localEndPoint.incomingCall(this);
		}
	}

	// Used by transactions CInvite and SAck to inform when the call has set up
	// has completed
	public void completedCall() {

		if (request2Terminate) {
			// Call terminate request arrived between 200 OK response and ACK
			// 1.- CANCEL request, either remote or local, arrived after 200 OK
			// 2.- Error found.Normally associated to media
			// 3.- Terminate request due to lack of ACK (symmetric NAT problem)

			if (!ContextState.TERMINATED.equals(state)) {
				// Terminate call not already terminated
				// Use terminated variable as dialog state does not change quick
				// enough
				try {
					log.debug("Inmediatelly terminate an already stablished call");
					stateTransition(ContextState.TERMINATED);
					new CBye(this);
				} catch (ServerInternalErrorException e) {
					String msg = "Unable to terminate CALL for dialog: "
							+ dialog.getDialogId();
					log.error(msg, e);
					callFailed(msg);
				}
				release();
			}
			return;

		} else if (networkConnection != null) {
			try {
				stateTransition(ContextState.CONFIRMED);
				networkConnection.confirm();
				mediaTypesModes = getModesOfMediaTypes();
				notifySipCallEvent(CallEvent.CALL_SETUP);
			} catch (MsControlException e) {
				String msg = "Unable to set up media session. Terminate call";
				log.error(msg, e);
				callFailed(msg);
			}
		} else {
			// No network connection???
			String msg = "Null network connection found before declaring Call setup";
			log.error(msg);
			callFailed(msg);
		}
		// Remove reference to the initiating transactions (might be in or out)
//		incomingInitiatingRequest = null;
//		outgoingInitiatingRequest = null;
	}

	// Transaction completed the call negotiation with an error that will
	// prevent media transfer. request termination and call completedCall
	public void completedCallWithError(String msg) {
		request2Terminate = true;
		completedCall();
	}

	public void callError(String msg) {
		notifySipCallEvent(CallEvent.CALL_ERROR, msg);
		terminatedCall();
	}

	public void canceledCall() {
		notifySipCallEvent(CallEvent.CALL_CANCEL);
		terminatedCall();
	}

	public void busyCall() {
		notifySipCallEvent(CallEvent.CALL_BUSY);
		terminatedCall();
	}

	public void rejectedCall() {
		notifySipCallEvent(CallEvent.CALL_REJECT);
		terminatedCall();
	}

	public void ringingCall() {
		notifySipCallEvent(CallEvent.CALL_RINGING);
	}

	public void unsupportedMediaType() {
		notifySipCallEvent(CallEvent.MEDIA_NOT_SUPPORTED);
		terminatedCall();
	}

	public void userNotFound() {
		notifySipCallEvent(CallEvent.USER_NOT_FOUND);
		terminatedCall();
	}

	public void unsupportedCode() {
		notifySipCallEvent(CallEvent.USER_NOT_FOUND);
		terminatedCall();
	}
	
	public void callTimeout() {
		notifySipCallEvent(CallEvent.CALL_ERROR, "Protocol transaction timeout");
		terminatedCall();
	}

	public void terminatedCall() {
		request2Terminate = true;
		stateTransition(ContextState.TERMINATED);
		release();
		notifySipCallEvent(CallEvent.CALL_TERMINATE);
	}

	private void notifySipCallEvent(CallEventEnum eventType) {
		notifySipCallEvent(eventType, "");
	}

	private void notifySipCallEvent(CallEventEnum eventType, String msg) {
		// Notify call events when dialog are not complete
		if (callListener != null) {
			CallEvent event = new CallEvent(eventType, msg, this);
			try {
				callListener.onEvent(event);
			} catch (Exception e) {
				log.error("Exception throwed on listener.", e);
			}
		}
	}

	// /////////////////////
	//
	// Helper functions
	//
	// //////////////////////

	private String getCallInfo() {
		String local = localEndPoint!=null?localEndPoint.getUri():"???";
		String remote = remoteParty!=null?remoteParty.toString():"???";
		String arrow;
		if (incomingInitiatingRequest != null)
			arrow = " <<< ";
		else
			arrow = " >>> ";
		return local + arrow + remote;
		
	}
	
	private void stateTransition(ContextState newState) {
		
		log.debug("--------- SIP CONTEXT STATE TRANSITION ");
		log.debug("| " + getCallInfo() + ": " + state + " ---> "
				+ newState);
		state = newState;
	}

	// Used internally by Context to signal internal error conditions precenting
	// call setup

	private void callFailed(String msg) {
		notifySipCallEvent(CallEvent.CALL_ERROR, msg);
		terminatedCall();
	}

	private void localCallCancel() throws ServerInternalErrorException {
		
		// Create cancel request
		Request cancelReq;
		try {
			cancelReq = outgoingInitiatingRequest.getClientTransaction()
					.createCancel();
		} catch (SipException e1) {
			String msg = "Unable to cancel call locally";
			callFailed(msg);
			throw new ServerInternalErrorException(msg,e1);
		}
		
		// Send cancel request
		try {
			new CCancel(cancelReq, this);
			// Do not notify. Wait for reception of response 487
		} catch (ServerInternalErrorException e) {
			String msg="To late to cancel: " + getCallInfo();
			log.info(msg);
			// Try bye
			try {
				new CBye(this);
			} catch (ServerInternalErrorException e1) {
				String msg1 = "Unable to terminate call locally canceled:" + getCallInfo();
				callFailed(msg1);
				throw new ServerInternalErrorException(msg,e);
			}
		}
			
	}

	private void release() {
		if (networkConnection != null) {
			networkConnection.release();
			networkConnection = null;
		}
	}

	private void createSdpPortManager() throws ServerInternalErrorException {

		if (networkConnection == null) {
			try {
				networkConnection = UaFactory.getMediaSession()
						.createNetworkConnection();
				sdpPortManager = networkConnection.getSdpPortManager();

			} catch (MsControlException e) {
				throw new ServerInternalErrorException(
						"Unable to allocate network resources", e);
			}
		}
	}

	private byte[] getLocalSdp() throws ServerInternalErrorException {
		if (sdpPortManager == null)
			throw new ServerInternalErrorException(
					"Null SDP port manager found when requesting Session Descriptor");
		try {
			return SdpConversor.sessionSpec2Sdp(
					sdpPortManager.getMediaServerSessionDescription())
					.getBytes();
		} catch (Exception e) {
			throw new ServerInternalErrorException(
					"Unable to retrieve local Session Description", e);
		}
	}

}
