package com.kurento.commons.sip.agent;

import java.util.Map;

import javax.sdp.SdpException;
import javax.sip.Dialog;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kurento.commons.media.format.SessionSpec;
import com.kurento.commons.media.format.SpecTools;
import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.join.JoinableStream.StreamType;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.sdp.enums.MediaType;
import com.kurento.commons.sdp.enums.Mode;
import com.kurento.commons.sip.SipCall;
import com.kurento.commons.sip.SipCallListener;
import com.kurento.commons.sip.event.SipCallEvent;
import com.kurento.commons.sip.event.SipEventType;
import com.kurento.commons.sip.exception.ServerInternalErrorException;
import com.kurento.commons.sip.transaction.CBye;
import com.kurento.commons.sip.transaction.CCancel;
import com.kurento.commons.sip.transaction.CInvite;
import com.kurento.commons.sip.transaction.CTransaction;
import com.kurento.commons.sip.transaction.SInvite;
import com.kurento.commons.sip.transaction.STransaction;
import com.kurento.commons.sip.transaction.Transaction;

public class SipContext implements SipCall {

	protected static Log log = LogFactory.getLog(SipContext.class);

	private Dialog dialog;
	private Request cancelRequest;

	private SipEndPointImpl localEndPoint;
	private Address remoteParty;

	private STransaction incomingPendingRequest;
	// private CTransaction outgoingPendingRequest;

	private NetworkConnection networkConnection;
	private Map<MediaType, Mode> mediaTypesModes;

	private SipCallListener callListener;
	private boolean outgoingRequestCancelled;
	// private boolean isComplete = false;

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

	// ////////////////////
	//
	// SIP CALL INTERFACE
	//
	// ////////////////////

	@Override
	public void accept() throws ServerInternalErrorException {
		// Accept only if there is a pending transaction
		log.debug("Accept Call: Check if there is a pending transaction");
		if (incomingPendingRequest == null) {
			throw new ServerInternalErrorException(
					"Bad accept. There isn't a pending request to be accepted");
		}

		// Send ACCEPT RESPONSE
		incomingPendingRequest.sendResponse(Response.OK,
				incomingPendingRequest.getLocalSdp());

	}

	@Override
	public void reject() throws ServerInternalErrorException {
		// Reject only if there is a pending transaction
		log.debug("Reject Call: Check if there is a pending transaction");
		if (incomingPendingRequest == null) {
			throw new ServerInternalErrorException(
					"Bad reject. There isn't a pending request to be accepted");
		}

		// Send DECLINE response
		incomingPendingRequest.sendResponse(Response.DECLINE, null);
		incomingPendingRequest = null;
	}

	@Override
	public void hangup() {
		log.info("Request to terminate callId: " + dialog.getCallId());
		try {
			new CBye(this);
		} catch (ServerInternalErrorException e) {
			log.warn("Unable to send BYE request", e);
		}
		if (networkConnection != null) {
			networkConnection.release();
			networkConnection = null;
		}
	}

	@Override
	public void cancel() throws ServerInternalErrorException {
		log.info("Request to cancel callId: " + dialog.getCallId());
		if (cancelRequest == null)
			throw new ServerInternalErrorException("No request for cancel.");

		try {
			new CCancel(cancelRequest, this);
			outgoingRequestCancelled = true;
		} catch (ServerInternalErrorException e) {
			log.warn("Unable to send CANCEL request", e);
		}
		if (networkConnection != null) {
			networkConnection.release();
			networkConnection = null;
		}
	}

	@Override
	public void addListener(SipCallListener listener) {
		callListener = listener;
	}

	@Override
	public void removeListener(SipCallListener listener) {
		callListener = null;
	}

	@Override
	public NetworkConnection getNetworkConnection(StreamType media) {
		return networkConnection;
	}

	@Override
	public Map<MediaType, Mode> getMediaTypesModes() {
		return mediaTypesModes;
	}

	@Override
	public Boolean isConnected() {
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

	// /////////////////////
	//
	// Sip End Point API
	//
	// //////////////////////

	protected void connect(Address remoteParty)
			throws ServerInternalErrorException {
		this.remoteParty = remoteParty;
		log.info("Request connection from" + localEndPoint.getAddress()
				+ " => To => " + remoteParty);
		// Create transaction INVITE
		CInvite invite = new CInvite(this);
		dialog = invite.getClientTransaction().getDialog();
		dialog.setApplicationData(this);

		try {
			cancelRequest = invite.getClientTransaction().createCancel();
			outgoingRequestCancelled = false;
		} catch (SipException e) {
			log.error(
					"Unable to generate CANCEL request to use it in the future",
					e);
		}
	}

	// ////////////////////
	//
	// Transaction Interface
	//
	// ////////////////////

	public void incominCall(SInvite pendingInvite,
			Map<MediaType, Mode> mediaTypesModes) {
		// Store pending request
		log.info("Incoming call signalled with callId:"
				+ pendingInvite.getServerTransaction().getDialog().getCallId());
		this.incomingPendingRequest = pendingInvite;
		this.remoteParty = this.incomingPendingRequest.getServerTransaction()
				.getDialog().getRemoteParty();
		this.mediaTypesModes = mediaTypesModes;

		// Notify the incoming call to EndPoint controllers
		log.info("Notify incoming call to EndPoint listener");
		localEndPoint.incomingCall(this);
	}

	public void rejectedCall() {
		notifySipCallEvent(SipCallEvent.CALL_REJECT);
	}

	public void failedCall() {
		notifySipCallEvent(SipCallEvent.CALL_ERROR);
	}

	public void terminatedCall() {
		notifySipCallEvent(SipCallEvent.CALL_TERMINATE);
		if (networkConnection != null) {
			networkConnection.release();
			networkConnection = null;
		}
	}

	public void completedIncomingCall() {
		log.debug("Incoming Call setup, callId: " + dialog.getCallId());
		// Set up connection
		completedCall(incomingPendingRequest);
		incomingPendingRequest = null;
	}

	public void completedOutgoingCall(CTransaction outgoingPendingRequest) {
		log.debug("Outgoing Call setup, callId: " + dialog.getCallId());
		// this.outgoingPendingRequest = outgoingPendingRequest;
		completedCall(outgoingPendingRequest);
	}

	private void completedCall(Transaction transaction) {
		boolean hangup = false;

		if (networkConnection != null) {
			// Release previous connection
			log.debug("Release old network connection");
			networkConnection.release();
			networkConnection = null;
		}

		// Get active networkConnection
		log.debug("Get network connection");
		if (!(transaction != null && (networkConnection = transaction
				.getNetworkConnection()) != null)) {
			// Really bad
			log.error("Unable to find a network connection for the call. Terminate call");
			hangup = true;
		} else {
			try {
				networkConnection.confirm();
				SessionSpec session;
				try {
					session = new SessionSpec(new String(networkConnection
							.getSdpPortManager()
							.getMediaServerSessionDescription()));

					this.mediaTypesModes = SpecTools
							.getModesOfFirstMediaTypes(session);
				} catch (SdpException e) {
					log.error("Unable to get local SDP. Terminate call", e);
					hangup = true;
				}

			} catch (MsControlException e) {
				log.error("Unable to set up media session. Terminate call", e);
				hangup = true;
			}
		}

		// Notify listeners
		notifySipCallEvent(SipCallEvent.CALL_SETUP);
		if (hangup)
			this.hangup();
	}

	public void notifySipCallEvent(SipEventType eventType) {
		// Notify call events when dialog are not complete
		if (callListener != null) {
			SipCallEvent event = new SipCallEvent(eventType, this);
			callListener.onEvent(event);
		}
	}

	public void cancelCall() throws ServerInternalErrorException {
		log.info("Cancel Call");
		if (incomingPendingRequest != null) {
			incomingPendingRequest.sendResponse(Response.REQUEST_TERMINATED, null);
			// pendingRequest.cancel();
			incomingPendingRequest = null;
			if (networkConnection != null) {
				networkConnection.release();
				networkConnection = null;
			}
			notifySipCallEvent(SipCallEvent.CALL_CANCEL);
		} else  throw new ServerInternalErrorException("Cancel call error, there are not pending request");
		
	}
	
	public boolean hasPendingTransaction() {
		return outgoingRequestCancelled;
	}
}
