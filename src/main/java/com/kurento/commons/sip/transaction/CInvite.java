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
package com.kurento.commons.sip.transaction;

import javax.sdp.SdpException;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.TimeoutEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.kurento.commons.media.format.conversor.SdpConversor;
import com.kurento.commons.media.format.exceptions.ArgumentNotSetException;
import com.kurento.commons.mscontrol.EventType;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.sip.agent.SipContext;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public class CInvite extends CTransaction {

	public CInvite(SipContext sipContext) throws ServerInternalErrorException {
		super(Request.INVITE, sipContext.getEndPoint(), sipContext
				.getRemoteParty());

		this.sipContext = sipContext;

		// INVITE REQUIRES TO INCREASE SEQUENCE NUMBER
		CTransaction.cSeqNumber++;

		// Add special headers for INVITE
		// In the android implementation the contact header is added
		// automatically and adding it again causes an error
		// request.addHeader(buildContactHeader()); // Contact
		request.addHeader(buildAllowHeader()); // Allow
		request.addHeader(buildSupportedHeader()); // SupportHeader

		// When SDP is generated an event is SENT to this transaction and
		// request is sent
		generateSdp();
	}

	@Override
	public void processResponse(ResponseEvent event)
			throws ServerInternalErrorException {
		Response response = event.getResponse();
		int statusCode = response.getStatusCode();
		log.info("processResponse: " + statusCode + " dialog: " + this.dialog
				+ ", state: " + dialog.getState());
		// Processing response
		if (statusCode == Response.TRYING) {
			log.info("<<<<<<< 100 TRYING: dialog: " + this.dialog + ", state: "
					+ dialog.getState());

		} else if (statusCode == Response.RINGING) {
			log.info("<<<<<<< 180 Ringing: dialog: " + this.dialog
					+ ", state: " + dialog.getState());

		} else if (statusCode == Response.SESSION_PROGRESS) {
			log.info("<<<<<<< 183 Session Progress: dialog: " + this.dialog
					+ ", state: " + dialog.getState());

		} else if (statusCode < 200) {
			log.info("<<<<<<< " + statusCode + " 1xx: dialog: " + this.dialog
					+ ", state: " + dialog.getState());
		} else if (statusCode == Response.REQUEST_TERMINATED) {
			log.info("<<<<<<< " + statusCode + " TERMINATED: dialog: "
					+ this.dialog + ", state: " + dialog.getState());
			sendAck();
			release();
		} else if (statusCode == Response.TEMPORARILY_UNAVAILABLE
				|| statusCode == Response.NOT_ACCEPTABLE_HERE
				|| statusCode == Response.BUSY_HERE
				|| statusCode == Response.BUSY_EVERYWHERE
				|| statusCode == Response.NOT_ACCEPTABLE
				|| statusCode == Response.DECLINE) {
			log.info("<<<<<<< " + statusCode + " REJECT: dialog: "
					+ this.dialog + ", state: " + dialog.getState());
			sipContext.rejectedCall();
			sendAck();
			release();

		} else if (statusCode == Response.OK) {
			// 200 OK
			log.info("<<<<<<< 200 OK: dialog: " + this.dialog + ", state: "
					+ dialog.getState());
			if (sipContext.hasPendingTransaction()) {
				sendAck();
				new CBye(sipContext);
			} else {
				byte[] rawContent = response.getRawContent();
				int l = response.getContentLength().getContentLength();
				if (l != 0 && rawContent != null) {
					// SDP offer sent by invite request
					log.debug("Process SDP response from remote peer");
					processSdpAnswer(rawContent);
				} else {
					log.error("Found response to CInvite with no SDP answer");
					sipContext.failedCall();
					sendAck();
					new CBye(sipContext);
				}
			}
		} else {
			log.info("<<<<<<< " + statusCode + " FAIL: dialog: " + this.dialog
					+ ", state: " + dialog.getState());
			sipContext.failedCall();
			sendAck();
			release();
		}
	}

	private void sendAck() throws ServerInternalErrorException {
		log.info("dialog.getState(): " + dialog.getState());
		if (!DialogState.CONFIRMED.equals(dialog.getState()))
			return;
		try {
			// Send ACK
			Request ackRequest = dialog.createAck(((CSeqHeader) request
					.getHeader(CSeqHeader.NAME)).getSeqNumber());
			dialog.sendAck(ackRequest);
			log.info("SIP send ACK\n" + ">>>>>>>>>> SIP send ACK >>>>>>>>>>\n"
					+ ackRequest.toString() + "\n"
					+ ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		} catch (InvalidArgumentException e) {
			release();
			throw new ServerInternalErrorException(
					"Invalid Argument Exception while sending ACK for transaction: "
							+ this.dialog, e);
		} catch (SipException e) {
			release();
			throw new ServerInternalErrorException(
					"Sip Exception while sending ACK for transaction: "
							+ this.dialog, e);
		}
	}

	@Override
	public void processTimeOut(TimeoutEvent timeoutEvent) {
		super.processTimeOut(timeoutEvent);
		log.error("Time Out while waiting a response from INVITE");
		sipContext.failedCall();
	}

	@Override
	public void onEvent(SdpPortManagerEvent event) {
		// Remove this transaction as a listener of the SDP Port Manager
		event.getSource().removeListener(this);

		EventType eventType = event.getEventType();
		try {
			if (SdpPortManagerEvent.OFFER_GENERATED.equals(eventType)) {
				// Request user confirmation before sending response
				log.debug("SdpPortManager successfully generated a SDP to be send to remote peer");
				try {
					sendRequest(SdpConversor.sessionSpec2Sdp(
							event.getMediaServerSdp()).getBytes());
				} catch (SdpException e) {
					log.warn("Unable to get local SDP", e);
				}
			} else if (SdpPortManagerEvent.ANSWER_PROCESSED.equals(eventType)) {
				// Notify call set up
				log.debug("SdpPortManager successfully processed SDP answer received from remote peer");
				sendAck();
				sipContext.completedOutgoingCall(this);
			} else {
				super.onEvent(event);
			}
		} catch (ServerInternalErrorException e) {
			log.error("Server error while managing SdpPortManagerEvent:"
					+ eventType, e);
			sipContext.failedCall();
			release();
		}
	}
}
