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
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.media.format.MediaSpec;
import com.kurento.commons.media.format.SessionSpec;
import com.kurento.commons.media.format.enums.MediaType;
import com.kurento.commons.media.format.enums.Mode;
import com.kurento.commons.media.format.exceptions.ArgumentNotSetException;
import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.join.Joinable;
import com.kurento.commons.mscontrol.join.JoinableStream.StreamType;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.sip.transaction.CBye;
import com.kurento.commons.sip.transaction.CCancel;
import com.kurento.commons.sip.transaction.CInvite;
import com.kurento.commons.sip.transaction.CTransaction;
import com.kurento.commons.sip.transaction.SInvite;
import com.kurento.commons.sip.transaction.STransaction;
import com.kurento.commons.sip.transaction.Transaction;
import com.kurento.commons.ua.Call;
import com.kurento.commons.ua.CallListener;
import com.kurento.commons.ua.event.CallEvent;
import com.kurento.commons.ua.event.CallEventEnum;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public class SipContext implements Call {

	protected static final Logger log = LoggerFactory
			.getLogger(SipContext.class);

	private Dialog dialog;
	private Request cancelRequest;

	private SipEndPointImpl localEndPoint;
	private Address remoteParty;

	private STransaction incomingPendingRequest;
	// private CTransaction outgoingPendingRequest;

	private NetworkConnection networkConnection;
	private Map<MediaType, Mode> mediaTypesModes;

	private CallListener callListener;
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

		if (networkConnection != null) {
			networkConnection.release();
			networkConnection = null;
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
			throw new ServerInternalErrorException("Cancel request is null");

		new CCancel(cancelRequest, this);
		outgoingRequestCancelled = true;

		if (networkConnection != null) {
			networkConnection.release();
			networkConnection = null;
		}
	}

	@Override
	public void addListener(CallListener listener) {
		callListener = listener;
	}

	@Override
	public void removeListener(CallListener listener) {
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
		log.info("Request connection from " + localEndPoint.getAddress()
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

	public void incominCall(SInvite pendingInvite, SessionSpec session) {
		// Store pending request
		log.info("Incoming call signalled with callId:"
				+ pendingInvite.getServerTransaction().getDialog().getCallId());
		this.incomingPendingRequest = pendingInvite;
		this.remoteParty = this.incomingPendingRequest.getServerTransaction()
				.getDialog().getRemoteParty();
		this.mediaTypesModes = getModesOfMediaTypes(session);
		this.networkConnection = incomingPendingRequest.getNetworkConnection();

		// Notify the incoming call to EndPoint controllers
		log.info("Notify incoming call to EndPoint listener");
		localEndPoint.incomingCall(this);
	}

	public void rejectedCall() {
		notifySipCallEvent(CallEvent.CALL_REJECT);
		terminatedCall();
	}

	public void failedCall() {
		notifySipCallEvent(CallEvent.CALL_ERROR);
		terminatedCall();
	}

	public void unsupportedMediaType() {
		notifySipCallEvent(CallEvent.MEDIA_NOT_SUPPORTED);
		terminatedCall();
	}

	public void userNotFound() {
		notifySipCallEvent(CallEvent.USER_NOT_FOUND);
		terminatedCall();
	}

	public void terminatedCall() {
		notifySipCallEvent(CallEvent.CALL_TERMINATE);
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

		// if (networkConnection != null) {
		// // Release previous connection
		// log.debug("Release old network connection");
		// networkConnection.release();
		// networkConnection = null;
		// }

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
				SessionSpec session = networkConnection.getSdpPortManager()
						.getMediaServerSessionDescription();
				this.mediaTypesModes = getModesOfMediaTypes(session);
			} catch (MsControlException e) {
				log.error("Unable to set up media session. Terminate call", e);
				hangup = true;
			}
		}

		// Notify listeners
		notifySipCallEvent(CallEvent.CALL_SETUP);
		if (hangup)
			this.hangup();
	}

	private void notifySipCallEvent(CallEventEnum eventType) {
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

	public void cancelCall() throws ServerInternalErrorException {
		log.info("Cancel Call");
		if (incomingPendingRequest != null) {
			incomingPendingRequest.sendResponse(Response.REQUEST_TERMINATED,
					null);
			// pendingRequest.cancel();
			incomingPendingRequest = null;
			if (networkConnection != null) {
				networkConnection.release();
				networkConnection = null;
			}
			notifySipCallEvent(CallEvent.CALL_CANCEL);
		} else
			throw new ServerInternalErrorException(
					"Cancel call error, there are not pending request");

	}

	public boolean isCancelled() {
		return outgoingRequestCancelled;
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
		// TODO Auto-generated method stub
		return null;
	}

	private static Map<MediaType, Mode> getModesOfMediaTypes(SessionSpec session) {
		Map<MediaType, Mode> map = new HashMap<MediaType, Mode>();
		if (session == null)
			return map;
		for (MediaSpec m : session.getMediaSpecs()) {
			try {
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

			} catch (ArgumentNotSetException e) {

			}
		}
		return map;
	}

}
