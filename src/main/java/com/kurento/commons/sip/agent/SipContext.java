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
import javax.sip.address.Address;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.media.format.MediaSpec;
import com.kurento.commons.media.format.SessionSpec;
import com.kurento.commons.media.format.conversor.SdpConversor;
import com.kurento.commons.media.format.enums.MediaType;
import com.kurento.commons.media.format.enums.Mode;
import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.join.Joinable;
import com.kurento.commons.mscontrol.join.JoinableStream.StreamType;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManager;
import com.kurento.commons.sip.transaction.CBye;
import com.kurento.commons.sip.transaction.CCancel;
import com.kurento.commons.sip.transaction.CInvite;
import com.kurento.commons.sip.transaction.CTransaction;
import com.kurento.commons.sip.transaction.STransaction;
import com.kurento.commons.ua.Call;
import com.kurento.commons.ua.CallListener;
import com.kurento.commons.ua.TerminateReason;
import com.kurento.commons.ua.event.CallEvent;
import com.kurento.commons.ua.event.CallEventEnum;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public class SipContext implements Call {

	// private enum OutState {
	// IDLE, SDP_OFFER_GEN, REQUEST_SENT, RESPONSE_RECV, SDP_ANSWER_PROC,
	// COMPLETE
	// };
	//
	// private enum InState {
	// IDLE, SDP_OFFER_PROC, WAIT_TU, RESPONSE_SENT, COMPLETE
	// }

	protected static final Logger log = LoggerFactory
			.getLogger(SipContext.class);

	private SipEndPointImpl localEndPoint;
	private Address remoteParty;

	private Dialog dialog;
	private NetworkConnection networkConnection;
	private SdpPortManager sdpPortManager;

	private STransaction incomingInitiatingRequest;
	private CTransaction outgoingInitiatingRequest;
	private Boolean request2Terminate = false;
	// private OutState outState = OutState.IDLE;
	// private InState inState = InState.IDLE;

	private CallListener callListener;
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

	// ////////////////////
	//
	// SIP CALL INTERFACE
	//
	// ////////////////////

	@Override
	public void accept() throws ServerInternalErrorException {
		// Accept only if there are incoming transactions
		log.debug("Accept Call: Check if there is a pending transaction");

		if (dialog == null) {
			throw new ServerInternalErrorException(
					"Bad accept. Unable to find a dialog for this call");
		}

		if (incomingInitiatingRequest == null) {
			throw new ServerInternalErrorException(
					"Bad accept. There isn't a incoming request to be accepted");
		}

		// Accept only when dialog is in early state
		// TODO: Will it be required to accept transactions when dialog is
		// in
		// confirmed state?. This will happen when in-dialog INVITE requests
		// are
		// received
		if (!DialogState.EARLY.equals(dialog.getState())) {
			throw new ServerInternalErrorException(
					"Bad accept. Dialog state is not EARLY. dialog="
							+ dialog.getState());
		}

		// Send response if not already canceled
		if (!request2Terminate)
			incomingInitiatingRequest.sendResponse(Response.OK, getLocalSdp());

	}

	@Override
	public void terminate() throws ServerInternalErrorException {
		terminate(TerminateReason.DECLINE);

	}

	@Override
	public void terminate(TerminateReason code)
			throws ServerInternalErrorException {

		log.info("Request to terminate call with code:" + code);

		// Label this context to be terminated as soon as possible
		request2Terminate = true;

		// Check valid states where a call can be canceled
		if (dialog == null) {
			// Dialog can be null before the INVITE request is sent
			// DO NOTHING: Wait until SDP offer is generated and prevent
			// INVITE to be sent
			log.debug("Request to terminate outgoing call with no INVITE transaction created yet");

		} else if (dialog.getState() == null) {
			// Dialog state is null until INVITE request is sent
			// DO NOTHING: INVITE will be sent and immediately canceled
			log.debug("Request to terminate outgoing call with no SDP offer generated yet");

		} else if (DialogState.EARLY.equals(dialog.getState())
				&& outgoingInitiatingRequest != null) {
			// Hang out an outgoing call after INVITE request is sent and
			// before response is received
			log.debug("Request to terminate pending outgoing call");
			// Send cancel request
			localCallCancel();

		} else if (DialogState.CONFIRMED.equals(dialog.getState())) {
			// Terminate request after 200 OK response. ACK might still not being received
			log.debug("Request to terminate established call");
			new CBye(this);

		} else if (DialogState.EARLY.equals(dialog.getState())
				&& incomingInitiatingRequest != null) {
			// TU requested CALL reject
			log.debug("Request to reject incoming call");
			// This code competes with the remote cancel. First one to execute
			// will cause the other to throw an exception avoiding duplicate
			// events
			if (TerminateReason.BUSY.equals(code)) {
				incomingInitiatingRequest
						.sendResponse(Response.BUSY_HERE, null);
			} else {
				incomingInitiatingRequest.sendResponse(Response.DECLINE, null);
			}
			rejectedCall();
		} 

		// Do not accept call to this method
		else {
			throw new ServerInternalErrorException(
					"Bad hangup. Unable to hangup a call");
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
	public Joinable getJoinable(StreamType media) {
		try {
			return networkConnection.getJoinableStream(media);
		} catch (MsControlException e) {
			return null;
		}
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
					m.getTransport().getRtp();

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
		if (incomingInitiatingRequest != null) {
			// Cancel received after SDP offer has been process
			// Send now the response
			// If 200OK response is already sent it will throw an exception and
			// no cancel notification will be sent to local party. The call will
			// progress normally
			incomingInitiatingRequest.sendResponse(Response.REQUEST_TERMINATED,
					null);
			canceledCall();
			// Remove reference to the initiating request
			incomingInitiatingRequest = null;
		} else {
			// Cancel received before the SDP has been processed
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

		if (request2Terminate) {
			// This condition can verify when a remote CANCEL request is
			// received before the SDP offer of incoming INVITE is still being
			// processed
			// Force call cancel and do not signal incoming to the controller
			try {
				incomingTransaction.sendResponse(Response.REQUEST_TERMINATED,
						null);
				canceledCall();
			} catch (ServerInternalErrorException e) {
				log.warn("Unable to terminate call canceled by remote party", e);
				// Controller doesn't know about this call. Do not signall
				// anything
			}
			release();
			return;
		}

		// Store pending request
		log.info("Incoming call signalled with callId:"
				+ incomingTransaction.getServerTransaction().getDialog()
						.getCallId());
		this.remoteParty = incomingTransaction.getServerTransaction()
				.getDialog().getRemoteParty();
		this.mediaTypesModes = getModesOfMediaTypes();
		this.incomingInitiatingRequest = incomingTransaction;

		// Notify the incoming call to EndPoint controllers and waits for
		// response (accept or reject)
		log.info("Notify incoming call to EndPoint listener");
		localEndPoint.incomingCall(this);
	}

	// Used by transactions CInvite and SAck to inform when the call has set up
	// has completed
	public void completedCall() {
		if (request2Terminate) {
			// Call terminate request arrived between 200 OK response and ACK
			// 1.- CANCEL request, either remote or local, arrived after 200 OK
			// 2.- Error found.Normally associated to media
			// 3.- Terminate request due to lack of ACK (symetric NAT problem)

			if (DialogState.CONFIRMED.equals(dialog.getState())) {
				// Terminate call only if dialog is still in confirmed state
				try {
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
		incomingInitiatingRequest = null;
		outgoingInitiatingRequest = null;
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

	public void terminatedCall() {
		release();
		notifySipCallEvent(CallEvent.CALL_TERMINATE);
	}

	private void notifySipCallEvent(CallEventEnum eventType) {
		notifySipCallEvent(eventType, "");
	}

	private void notifySipCallEvent(CallEventEnum eventType, String msg) {
		// Notify call events when dialog are not complete
		if (callListener != null) {
			CallEvent event = new CallEvent(eventType, this);
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

	// Used internally by Context to signal internal error conditions precenting
	// call setup

	private void callFailed(String msg) {
		notifySipCallEvent(CallEvent.CALL_ERROR, msg);
		terminatedCall();
	}

	private void localCallCancel() throws ServerInternalErrorException {
		try {
			new CCancel(outgoingInitiatingRequest.getClientTransaction()
					.createCancel(), this);
			// Do not notify. Wait for reception of response 487
		} catch (Exception e) {
			// Unable to complete signalling. Notify call error
			String msg = "Unable to create CANCEL request";
			callFailed(msg);
			throw new ServerInternalErrorException(msg, e);
		}
	}

	private void release() {
		incomingInitiatingRequest = null;
		outgoingInitiatingRequest = null;
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
