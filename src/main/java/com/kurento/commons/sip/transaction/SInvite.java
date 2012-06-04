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
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.media.format.conversor.SdpConversor;
import com.kurento.commons.mscontrol.EventType;
import com.kurento.commons.mscontrol.MediaEventListener;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerException;
import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.exception.SipTransactionException;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public class SInvite extends STransaction {

	private static Logger log = LoggerFactory.getLogger(SInvite.class);

	public SInvite(ServerTransaction serverTransaction,
			SipEndPointImpl localParty) throws ServerInternalErrorException,
			SipTransactionException {
		super(Request.INVITE, serverTransaction, localParty);

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
			sipContext.getSdpPortmanager().addListener(
					new MediaEventListener<SdpPortManagerEvent>() {

						@Override
						public void onEvent(SdpPortManagerEvent event) {
							event.getSource().removeListener(this);
							EventType eventType = event.getEventType();
							if (SdpPortManagerEvent.OFFER_GENERATED
									.equals(eventType)) {
								log.debug("SdpPortManager successfully generated a SDP to be send to remote peer");
								// Notify incoming call to TU
								sipContext.incominCall(SInvite.this);
							} else {
								// No caller listener available at this stage.
								// Do not send any error event
								log.debug("Unable to generate SDP offer to an empty incoming invite. SDP Port Manager event:"
										+ eventType);
								try {
									SInvite.this.sendResponse(
											Response.SERVICE_UNAVAILABLE, null);
								} catch (ServerInternalErrorException e) {
									log.error("Unable to terminate transaction for dialog: "
											+ dialog.getDialogId());
								}
							}

						}
					});
			try {
				sipContext.getSdpPortmanager().generateSdpOffer();
			} catch (SdpPortManagerException e) {
				String msg = "Unable to generate SDP offer for an incoming INVITE request";
				log.error(msg, e);
				sendResponse(Response.SERVER_INTERNAL_ERROR, null);
				// Do not signal incoming call to user
			}

		} else {
			// INVITE with SDP. request for process
			try {
				sipContext.getSdpPortmanager().addListener(
						new MediaEventListener<SdpPortManagerEvent>() {

							@Override
							public void onEvent(SdpPortManagerEvent event) {
								event.getSource().removeListener(this);
								EventType eventType = event.getEventType();
								if (SdpPortManagerEvent.ANSWER_GENERATED
										.equals(eventType)) {
									log.debug("SdpPortManager successfully processed SDP offer sent by peer");
									// Notify incoming call to TU
									SInvite.this.sipContext
											.incominCall(SInvite.this);

								} else {
									// No caller listener available at this stage.
									// Do not send any error event
									log.debug("Unable to process SDP offer to an incoming invite. SDP Port Manager event:"
											+ eventType);
									
									try {
										if (SdpPortManagerEvent.RESOURCE_UNAVAILABLE
												.equals(eventType)) {
											sendResponse(Response.SERVICE_UNAVAILABLE, null);

										} else if (SdpPortManagerEvent.SDP_NOT_ACCEPTABLE.equals(eventType)) {
											sendResponse(Response.UNSUPPORTED_MEDIA_TYPE, null); 

										} else {
											sendResponse(Response.SERVER_INTERNAL_ERROR, null);
										}
									} catch (Exception e) {
										log.error("Unable to send error response to an incoming invite", e);
									}
								}

							}
						});
				byte[] rawContent = request.getRawContent();
				sipContext.getSdpPortmanager().processSdpOffer(
						SdpConversor.sdp2SessionSpec(new String(rawContent)));
			} catch (Exception e) {
				String msg = "Unable to process SDP response";
				log.error(msg, e);
				sipContext.callError(msg);
			}
		}
	}

	private void processAck() {
		log.debug("Invite transaction received a valid ACK for non 2xx response");
	}

	private void sendErrorResponse(SdpPortManagerEvent event) {

		EventType eventType = event.getEventType();

		try {
			if (SdpPortManagerEvent.NETWORK_STREAM_FAILURE.equals(eventType)) {
				sendResponse(Response.SERVER_INTERNAL_ERROR, null);

			} else if (SdpPortManagerEvent.RESOURCE_UNAVAILABLE
					.equals(eventType)) {
				sendResponse(Response.SERVICE_UNAVAILABLE, null);

			} else if (SdpPortManagerEvent.SDP_GLARE.equals(eventType)) {

			} else if (SdpPortManagerEvent.SDP_NOT_ACCEPTABLE.equals(eventType)) {

			} else if (SdpPortManagerEvent.UNSOLICITED_OFFER_GENERATED
					.equals(eventType)) {

			} else {

			}
		} catch (Exception e) {
			log.error("Unable to send error response", e);
		}

	}
}
