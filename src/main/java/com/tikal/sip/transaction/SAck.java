package com.tikal.sip.transaction;

import javax.sip.ServerTransaction;
import javax.sip.message.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tikal.mscontrol.EventType;
import com.tikal.mscontrol.networkconnection.SdpPortManagerEvent;
import com.tikal.sip.agent.SipEndPointImpl;
import com.tikal.sip.exception.ServerInternalErrorException;
import com.tikal.sip.exception.SipTransactionException;

public class SAck extends STransaction {

	private static Log log = LogFactory.getLog(SAck.class);

	// Process ACK to a successful INVITE
	public SAck(ServerTransaction serverTransaction, SipEndPointImpl localParty)
			throws ServerInternalErrorException, SipTransactionException {
		super(Request.ACK,serverTransaction, localParty);

		// Process request
		Request request = serverTransaction.getRequest();

		log.debug(this.getClass().getSimpleName() + ": Invite transaction received a valid ACK");
		
		// Process ACK request
		if (getContentLength(request) == 0) {
			log.debug("ACK does not provides content. Call completes successfully");
			sipContext.completedIncomingCall();
		} else {
			log.debug("ACK contains SDP response.");
			processSdpAnswer(request.getRawContent());
		}
	}

	@Override
	public void onEvent(SdpPortManagerEvent event) {
		// Remove this transaction as a listener of the SDP Port Manager
		EventType eventType = event.getEventType();
		log.debug("SdpPortManager complete SDP process. Event received:" + eventType);
		event.getSource().removeListener(this);
		if (SdpPortManagerEvent.ANSWER_PROCESSED.equals(eventType)
				|| SdpPortManagerEvent.OFFER_GENERATED.equals(eventType)) {
				sipContext.completedIncomingCall();
		} else {
			super.onEvent(event);
		}
	}

}
