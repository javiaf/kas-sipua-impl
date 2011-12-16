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

import javax.sip.ServerTransaction;
import javax.sip.message.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kurento.commons.mscontrol.EventType;
import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.exception.ServerInternalErrorException;
import com.kurento.commons.sip.exception.SipTransactionException;

public class SAck extends STransaction {

	private static Log log = LogFactory.getLog(SAck.class);

	// Process ACK to a successful INVITE
	public SAck(ServerTransaction serverTransaction, SipEndPointImpl localParty)
			throws ServerInternalErrorException, SipTransactionException {
		super(Request.ACK, serverTransaction, localParty);

		// Process request
		Request request = serverTransaction.getRequest();

		log.debug(this.getClass().getSimpleName()
				+ ": Invite transaction received a valid ACK");

		// Process ACK request
		if (getContentLength(request) == 0) {
			log.debug("ACK does not provides content. Call completes successfully");
			sipContext.completedIncomingCall();
		} else {
			log.debug("ACK contains SDP response.");
			if (sipContext != null) {
				networkConnection = sipContext.getNetworkConnection(null);
				try {
					sdpPortManager = networkConnection.getSdpPortManager();
				} catch (MsControlException e) {
					throw new ServerInternalErrorException("SDP negociation error while processing answer", e);
				}
				processSdpAnswer(request.getRawContent());
			} else {
				throw new ServerInternalErrorException("Unable to find SipContext associated to transaction.");
			}
			
		}
	}

	@Override
	public void onEvent(SdpPortManagerEvent event) {
		// Remove this transaction as a listener of the SDP Port Manager
		EventType eventType = event.getEventType();
		log.debug("SdpPortManager complete SDP process. Event received:"
				+ eventType);
		event.getSource().removeListener(this);
		if (SdpPortManagerEvent.ANSWER_PROCESSED.equals(eventType)
				|| SdpPortManagerEvent.OFFER_GENERATED.equals(eventType)) {
			sipContext.completedIncomingCall();
		} else {
			super.onEvent(event);
		}
	}

}
