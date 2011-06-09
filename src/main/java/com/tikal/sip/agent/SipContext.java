package com.tikal.sip.agent;

import java.io.IOException;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javaxt.sip.Dialog;
import javaxt.sip.address.Address;
import javaxt.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tikal.sip.SipCall;
import com.tikal.sip.SipCallListener;
import com.tikal.sip.event.SipCallEvent;
import com.tikal.sip.event.SipEventType;
import com.tikal.sip.exception.ServerInternalErrorException;
import com.tikal.sip.transaction.CBye;
import com.tikal.sip.transaction.CInvite;
import com.tikal.sip.transaction.CTransaction;
import com.tikal.sip.transaction.SInvite;
import com.tikal.sip.transaction.STransaction;
import com.tikal.sip.transaction.Transaction;

public class SipContext implements SipCall {

	protected static Log log = LogFactory.getLog(SipContext.class);
	
	private Dialog dialog;
	
	private SipEndPointImpl localEndPoint;
	private Address remoteParty;
	
	private STransaction incomingPendingRequest;
	private CTransaction outgoingPendingRequest;
	
	private NetworkConnection networkConnection;
		
	private SipCallListener callListener;
	
//	private boolean isComplete = false;

	// ////////////////////
	//
	// Constructor
	//
	// ////////////////////
	
	public SipContext (SipEndPointImpl localEndPoint) {
		this(localEndPoint, null);
	}
	
	public SipContext (SipEndPointImpl localEndPoint, Dialog dialog) {
		this.localEndPoint = localEndPoint;
		this.dialog=dialog;
	}

	// ////////////////////
	//
	// GETTERS
	//
	// ////////////////////

	public Dialog getDialog () {
		return dialog;
	}
	
	public SipEndPointImpl getEndPoint (){
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
	
	@Override public void accept() throws ServerInternalErrorException {
		// Accept only if there is a pending transaction
		log.debug("Accept Call: Check if there is a pending transaction");
		if ( incomingPendingRequest == null) {
			throw new ServerInternalErrorException("Bad accept. There isn't a pending request to be accepted");
		}

		// Send ACCEPT RESPONSE
		incomingPendingRequest.sendResponse(Response.OK, incomingPendingRequest.getLocalSdp());

	}
	
	@Override public void reject() throws ServerInternalErrorException {
		// Reject only if there is a pending transaction
		log.debug("Reject Call: Check if there is a pending transaction");
		if ( incomingPendingRequest == null) {
			throw new ServerInternalErrorException("Bad reject. There isn't a pending request to be accepted");
		}
		
		//Send DECLINE response
		incomingPendingRequest.sendResponse(Response.DECLINE,null);
	}

	@Override
	public void hangup() {
		log.debug("Request to terminate callId: " + dialog.getCallId());
		try {
			new CBye(this);
		} catch (ServerInternalErrorException e) {
			log.warn("Unable to send BYE request", e);
		}
		if (networkConnection != null ) {
			networkConnection.release();
		}
	}
	
	@Override
	public void setListener (SipCallListener listener) {
		callListener = listener;
	}
	
	@Override
	public NetworkConnection getNetworkConnection(StreamType media) {
		return networkConnection;

	}

	@Override
	public Boolean isConnected() {
		return false;
	}
	
	///////////////////////
	//
	// Sip End Point API
	//
	////////////////////////
	
	protected void connect (Address remoteParty) throws ServerInternalErrorException {
		this.remoteParty = remoteParty;
		log.debug("Request connection from" + localEndPoint.getAddress() + " => To => " + remoteParty);
		// Create transaction INVITE
		CInvite invite = new CInvite(this);
		dialog = invite.getClientTransaction().getDialog();
		dialog.setApplicationData(this);
	}
	
	// ////////////////////
	//
	// Transaction Interface
	//
	// ////////////////////
	
	public void incominCall (SInvite pendingInvite){
		// Store pending request
		log.debug("Incoming call signalled with callId:" + pendingInvite.getServerTransaction().getDialog().getCallId());
		this.incomingPendingRequest = pendingInvite;

		// Notify the incoming call to EndPoint controllers
		log.debug("Notify incoming call to EndPoint listener");
		localEndPoint.incomingCall(this);
	}
	
	public void rejectedCall () {
		notifySipCallEvent(SipCallEvent.CALL_REJECT);
	}
	
	public void failedCall () {
		notifySipCallEvent(SipCallEvent.CALL_ERROR);
	}
	
	public void terminatedCall() {
		notifySipCallEvent(SipCallEvent.CALL_TERMINATE);
		if (networkConnection != null)
			networkConnection.release();
	}
	
	public void completedIncomingCall() {
		log.debug("Incoming Call setup, callId: " + dialog.getCallId());
		// Set up connection
		completedCall(incomingPendingRequest);
	}
	
	public void completedOutgoingCall (CTransaction outgoingPendingRequest) {
		log.debug("Outgoing Call setup, callId: " + dialog.getCallId());
		this.outgoingPendingRequest = outgoingPendingRequest;
		completedCall(outgoingPendingRequest);		
	}
	
	private void completedCall (Transaction transaction) {
		// Notify listeners
		notifySipCallEvent(SipCallEvent.CALL_SETUP);
		
		if (networkConnection != null) {
			// Release previous connection
			log.debug("Release old network connection");
			networkConnection.release();
		}
		
		// Get active networkConnection
		log.debug("Get network connection");
		if (!(transaction != null && (networkConnection=transaction.getNetworkConnection()) != null)) {
			// Really bad 
			log.error("Unable to find a network connection for the call. Terminate call");
			this.hangup();
		} else {
			try {
				networkConnection.confirm();
			} catch (MsControlException e) {
				log.error("Unable to set up media session. Terminate call",e);
				this.hangup();
			}
		}	
	}
	
	public void notifySipCallEvent (SipEventType eventType) {
		// Notify call events when dialog are not complete
		if ( callListener != null) {
			SipCallEvent event = new SipCallEvent(eventType, this);
			callListener.onEvent(event);
		}
	}
	
	public void cancelCall() throws IOException {
		if (incomingPendingRequest != null) {
//			pendingRequest.cancel();
			incomingPendingRequest = null;	
		}
	}

}
