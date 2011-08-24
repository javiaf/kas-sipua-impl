package com.kurento.commons.sip.transaction;

import javax.sdp.SdpException;
import javax.sip.ServerTransaction;
import javax.sip.TimeoutEvent;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kurento.commons.media.format.SessionSpec;
import com.kurento.commons.mscontrol.EventType;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.exception.ServerInternalErrorException;
import com.kurento.commons.sip.exception.SipTransactionException;

public class SInvite extends STransaction {

	private static Log log = LogFactory.getLog(SInvite.class);

	public SInvite(ServerTransaction serverTransaction, SipEndPointImpl localParty) throws ServerInternalErrorException, SipTransactionException {
		super(Request.INVITE,serverTransaction,localParty);

		// Process request
		Request request = serverTransaction.getRequest();
		if (request.getMethod().equals(Request.INVITE)) {
			log.debug("Process INVITE request");
			processInvite(request);
		} else if (request.getMethod().equals(Request.ACK)) {
			log.debug("Process ACK resquest");
			processAck();
		} else {
			log.error("Bad request. Method not allowed in a Server INVITE: "
					+ request.getMethod());
			sendResponse(Response.BAD_REQUEST, null);
		}
	}

	private void processInvite(Request request)
			throws ServerInternalErrorException, SipTransactionException {

		// Send RINGING
		sendResponse(Response.RINGING, null);

		// Process INVITE request
		if (getContentLength(request) == 0) {
			// INVITE with no SDP. Request an offer to send with response
			generateSdp();
		} else {
			// INVITE with SDP. request for process
			processSdpOffer(request.getRawContent());
		}
	}

	private void processAck() {
		log.debug("Invite transaction received a valid ACK for non 2xx response");
	}

	@Override
	public void processTimeOut(TimeoutEvent timeoutEvent) {
		log.error("Time Out while waiting for an ACK");
		sipContext.failedCall();
	}

	@Override
	public void onEvent(SdpPortManagerEvent event) {
		// Remove this transaction as a listener of the SDP Port Manager
		event.getSource().removeListener(this);
		SessionSpec ss;
		try {
			ss = new SessionSpec(new String(event.getMediaServerSdp()));
			localSdp = ss.getSessionDescription();
		} catch (SdpException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		EventType eventType = event.getEventType();
		
		if (SdpPortManagerEvent.ANSWER_GENERATED.equals(eventType)
				|| SdpPortManagerEvent.OFFER_GENERATED.equals(eventType)) {
			// Request user confirmation before sending response
			log.debug("SdpPortManager successfully generated a SDP to be send to remote peer");
			sipContext.incominCall(this);
		} else {
			super.onEvent(event);
		}
	}

}
