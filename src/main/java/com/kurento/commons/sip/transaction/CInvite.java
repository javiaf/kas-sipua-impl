package com.kurento.commons.sip.transaction;

import javax.sip.ClientTransaction;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionState;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.kurento.commons.mscontrol.EventType;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.sip.agent.SipContext;
import com.kurento.commons.sip.exception.ServerInternalErrorException;

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

		} else if (statusCode == Response.REQUEST_TERMINATED) {
			log.info("<<<<<<< " + statusCode + " TERMINATED: dialog: "
					+ this.dialog + ", state: " + dialog.getState());	
			sendAck(null);
			release();
		}
		else if ( statusCode == Response.TEMPORARILY_UNAVAILABLE
				|| statusCode == Response.NOT_ACCEPTABLE_HERE
				|| statusCode == Response.BUSY_HERE
				|| statusCode == Response.BUSY_EVERYWHERE
				|| statusCode == Response.NOT_ACCEPTABLE
				|| statusCode == Response.DECLINE) {
			log.info("<<<<<<< " + statusCode + " REJECT: dialog: "
					+ this.dialog + ", state: " + dialog.getState());
			sipContext.rejectedCall();
			sendAck(null);
			release();

		} else if (statusCode == Response.OK) {
			// 200 OK
			log.info("<<<<<<< 200 OK: dialog: " + this.dialog + ", state: "
					+ dialog.getState());
			if (!sipContext.hasPendingTransaction()) {
				sendAck(null);
				new CBye(sipContext);
			} else  {
				byte[] rawContent = response.getRawContent();
				int l = response.getContentLength().getContentLength();
				if (l != 0 && rawContent != null) {
					// SDP offer sent by invite request
					log.debug("Process SDP response from remote peer");
					processSdpAnswer(rawContent);
				} else {
					log.error("Found response to CInvite with no SDP answer");
					sipContext.failedCall();
				}
				sendAck(null);
			}
		} else {
			log.info("<<<<<<< " + statusCode + " FAIL: dialog: " + this.dialog
					+ ", state: " + dialog.getState());
			sipContext.failedCall();
			sendAck(null);
			release();
		}
	}

	private void sendAck(byte[] sdp) throws ServerInternalErrorException {
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
				sendRequest(event.getMediaServerSdp());

			} else if (SdpPortManagerEvent.ANSWER_PROCESSED.equals(eventType)) {
				// Notify call set up
				log.debug("SdpPortManager successfully processed SDP answer received from remote peer");
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
