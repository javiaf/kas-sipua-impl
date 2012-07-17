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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.media.format.conversor.SdpConversor;
import com.kurento.commons.mscontrol.EventType;
import com.kurento.commons.mscontrol.MediaEventListener;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.exception.SipTransactionException;
import com.kurento.ua.commons.ServerInternalErrorException;

public class SAck extends STransaction {

	private static Logger log = LoggerFactory.getLogger(SAck.class);

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
			sipContext.completedCall();
		} else {
			log.debug("ACK contains SDP response.");

			if (sipContext != null) {
				try {
					sipContext.getSdpPortmanager().addListener(
							new MediaEventListener<SdpPortManagerEvent>() {

								@Override
								public void onEvent(SdpPortManagerEvent event) {
									event.getSource().removeListener(this);
									EventType eventType = event.getEventType();
									if (SdpPortManagerEvent.ANSWER_PROCESSED
											.equals(eventType)) {
										log.debug("SdpPortManager successfully processed SDP offer sent by peer");
										// Notify incoming call to TU
										SAck.this.sipContext.completedCall();

									} else {
										// TODO: Analyze whether more detailed
										// information
										// is required of problem found
										// Get error cause
										String code;
										if (eventType == null)
											code = event.getError() +": " + event.getErrorText();
										else 
											code = eventType.toString();
										
										SAck.this.sipContext
												.callError("Unable to allocate network resources - "
														+ code);
									}
								}
							});
					byte[] rawContent = request.getRawContent();
					sipContext.getSdpPortmanager().processSdpAnswer(
							SdpConversor
									.sdp2SessionSpec(new String(rawContent)));
				} catch (Exception e) {
					String msg = "Unable to process SDP response";
					log.error(msg, e);
					sipContext.callError(msg);
				}
			} else {
				throw new ServerInternalErrorException(
						"Unable to find SipContext associated to transaction.");
			}

		}
	}

}
