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

import java.text.ParseException;

import javax.sdp.SdpException;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.kurento.commons.media.format.conversor.SdpConversor;
import com.kurento.commons.mscontrol.EventType;
import com.kurento.commons.mscontrol.MediaEventListener;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerException;
import com.kurento.commons.sip.agent.SipContext;
import com.kurento.commons.sip.agent.UaFactory;
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

		// Add SDP port manager listener to process SDP offer generation event
		sipContext.getSdpPortmanager().addListener(
				new MediaEventListener<SdpPortManagerEvent>() {

					@Override
					public void onEvent(SdpPortManagerEvent event) {
						event.getSource().removeListener(this);
						EventType eventType = event.getEventType();
						if (SdpPortManagerEvent.OFFER_GENERATED
								.equals(eventType)) {
							log.debug("SdpPortManager successfully generated a SDP to be send to remote peer");
							try {
								// Send the request
								CInvite.this.sendRequest(SdpConversor
										.sessionSpec2Sdp(
												event.getMediaServerSdp())
										.getBytes());
								// Notify the SIP context the request has been
								// sent
								CInvite.this.sipContext
										.outgoingCall(CInvite.this);
							} catch (SdpException e) {
								log.error("Unable to parse SDP offer", e);
								CInvite.this.sipContext
										.callError("Unable to convert media offer to a valid SDP string");
							} catch (ServerInternalErrorException e) {
								log.error("Unable to send INVITE request", e);
								CInvite.this.sipContext
										.callError("Unable to send INVITE request");
							}

							// No event generated with normal operation
						} else {
							// TODO: Analyze whether more detailed information
							// is required of problem found
							CInvite.this.sipContext
									.callError("Unable to allocate network resources. SdpPortManager event="
											+ eventType);
						}

					}
				});
		try {
			sipContext.getSdpPortmanager().generateSdpOffer();
		} catch (SdpPortManagerException e) {
			throw new ServerInternalErrorException(
					"Unable to generate SDP offer");
		}
	}

	@Override
	public void processResponse(ResponseEvent event) {
		Response response = event.getResponse();
		int statusCode = response.getStatusCode();
		log.info("processResponse: " + statusCode + " dialog: " + this.dialog
				+ ", state: " + dialog.getState());

		// Processing response
		if (statusCode == Response.TRYING) {
			log.info("<<<<<<< 100 TRYING: dialog: " + this.dialog + ", state: "
					+ dialog.getState());
			// DO NOTHING

		} else if (statusCode == Response.RINGING) {
			log.info("<<<<<<< 180 Ringing: dialog: " + this.dialog
					+ ", state: " + dialog.getState());
			// DO NOTHING
			sipContext.ringingCall();

		} else if (statusCode == Response.SESSION_PROGRESS) {
			log.info("<<<<<<< 183 Session Progress: dialog: " + this.dialog
					+ ", state: " + dialog.getState());
			// DO NOTHING

		} else if (statusCode < 200) {
			log.info("<<<<<<< " + statusCode + " 1xx: dialog: " + this.dialog
					+ ", state: " + dialog.getState());
			// DO NOTHING

		} else if (statusCode == Response.REQUEST_TERMINATED) {
			log.info("<<<<<<< " + statusCode + " TERMINATED: dialog: "
					+ this.dialog.getDialogId() + ", state: "
					+ dialog.getState());
			// Notify successfull call cancel
			log.info("<<<<<<< " + statusCode
					+ " Session cancel confirmed by remote peer: dialog: "
					+ this.dialog + ", state: " + dialog.getState());
			sipContext.canceledCall();

		} else if (	statusCode == Response.BUSY_HERE
				|| statusCode == Response.BUSY_EVERYWHERE ) {
			log.info("<<<<<<< " + statusCode
					+ "Remote peer is BUSY: dialog: " + this.dialog
					+ ", state: " + dialog.getState());
			sipContext.busyCall();

		} else if (statusCode == Response.TEMPORARILY_UNAVAILABLE

				|| statusCode == Response.DECLINE) {
			log.info("<<<<<<< " + statusCode
					+ "Session REJECT by remote peer: dialog: " + this.dialog
					+ ", state: " + dialog.getState());
			sipContext.rejectedCall();

		} else if (statusCode == Response.UNSUPPORTED_MEDIA_TYPE
				|| statusCode == Response.NOT_ACCEPTABLE_HERE
				|| statusCode == Response.NOT_ACCEPTABLE) {
			log.info("<<<<<<< " + statusCode
					+ " UNSUPPORTED_MEDIA_TYPE: dialog: " + this.dialog
					+ ", state: " + dialog.getState());
			sipContext.unsupportedMediaType();

		} else if (statusCode == 476 || statusCode == Response.NOT_FOUND) {
			// USER_NOT_FOUND. SIP/2.0 476
			// Unresolvable destination
			log.info("<<<<<<< " + statusCode + " USER_NOT_FOUND: dialog: "
					+ this.dialog + ", state: " + dialog.getState());
			sipContext.userNotFound();

		} else if (statusCode == Response.OK) {
			// 200 OK
			log.info("<<<<<<< 200 OK: dialog: " + this.dialog.getDialogId()
					+ ", state: " + dialog.getState());
			byte[] rawContent = response.getRawContent();
			int l = response.getContentLength().getContentLength();
			if (l != 0 && rawContent != null) {
				// SDP offer sent by invite request
				log.debug("Process SDP response from remote peer");
				processSdpAnswer(rawContent);
			} else {
				// Send ACK to complete INVITE transaction
				try {
					sendAck(null);
					sipContext.completedCallWithError("INVITE response received with no SDP");
				} catch (ServerInternalErrorException e) {
					String msg = "Unable to send ACK message";
					log.error(msg, e);
					sipContext.callError(msg);
				}
				sipContext.completedCallWithError("Received response to INVITE with no SDP");
			}
		} else if (statusCode > 200 && statusCode < 400) {
			// Unsupported codes
			log.info("<<<<<<< " + statusCode
					+ " Response code not supported : dialog: " + this.dialog
					+ ", state: " + dialog.getState());
			sipContext.completedCallWithError("Unssuported code:" + statusCode);
		} else {
			log.info("<<<<<<< " + statusCode
					+ " Response code not supported : dialog: " + this.dialog
					+ ", state: " + dialog.getState());
			sipContext.callError("Unsupported status code received:"
					+ statusCode);
			// sendAck(); // ACK is automatically sent by the SIP Stack for
			// codes >4xx
		}
	}

	private void sendAck(byte[] sdp) throws ServerInternalErrorException {
		// Non 2XX responses will cause the SIP Stack to send the ACK message
		// automatically
		if (!DialogState.CONFIRMED.equals(dialog.getState()))
			// Only dialogs in state confirm can send 200 OK ACKs
			return;
		try {
			// Send ACK
			Request ackRequest = dialog.createAck(((CSeqHeader) request
					.getHeader(CSeqHeader.NAME)).getSeqNumber());

			if (sdp != null) {
				ContentTypeHeader contentTypeHeader = UaFactory
						.getHeaderFactory().createContentTypeHeader(
								"application", "SDP");
				ackRequest.setContent(sdp, contentTypeHeader);
			}
			dialog.sendAck(ackRequest);
			log.info("SIP send ACK\n" + ">>>>>>>>>> SIP send ACK >>>>>>>>>>\n"
					+ ackRequest.toString() + "\n"
					+ ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		} catch (InvalidArgumentException e) {
			String msg = "Invalid Argument Exception while sending ACK for transaction: "
					+ this.dialog.getDialogId();
			throw new ServerInternalErrorException(msg, e);
		} catch (SipException e) {
			String msg = "Sip Exception while sending ACK for transaction: "
					+ this.dialog.getDialogId();
			throw new ServerInternalErrorException(msg, e);

		} catch (ParseException e) {
			String msg = "Unssupported SDP while sending ACK request for transaction: "
					+ this.dialog.getDialogId();
			throw new ServerInternalErrorException(msg, e);

		}
	}

	private void processSdpAnswer(byte[] rawContent) {
		// Add SDP port manager listener to process SDP answer event
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
								// Send ACK
								try {
									sendAck(null);
									CInvite.this.sipContext.completedCall();
								} catch (ServerInternalErrorException e) {
									String msg = "Unable to send ACK message after SDP processing";
									log.error(msg, e);
									CInvite.this.sipContext.callError(msg);
								}

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
								
								CInvite.this.sipContext
										.callError("Unable to allocate network resources - "
												+ code);
							}

						}
					});
			sipContext.getSdpPortmanager().processSdpAnswer(
					SdpConversor.sdp2SessionSpec(new String(rawContent)));
		} catch (Exception e) {
			String msg = "Unable to process SDP response";
			log.error(msg, e);
			sipContext.callError(msg);
		}
	}
}
