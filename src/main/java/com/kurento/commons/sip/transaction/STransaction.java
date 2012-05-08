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

import gov.nist.javax.sip.header.ContentLength;

import java.text.ParseException;

import javax.sdp.SdpException;
import javax.sdp.SessionDescription;
import javax.sip.ServerTransaction;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.media.format.SessionSpec;
import com.kurento.commons.media.format.conversor.SdpConversor;
import com.kurento.commons.mscontrol.EventType;
import com.kurento.commons.mscontrol.MediaErr;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.sip.agent.SipContext;
import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.exception.SipTransactionException;
import com.kurento.commons.sip.util.SipHeaderHelper;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public abstract class STransaction extends Transaction {

	protected static Logger log = LoggerFactory.getLogger(STransaction.class);

	protected ServerTransaction serverTransaction;

	protected STransaction(String method,ServerTransaction serverTransaction, SipEndPointImpl localParty) throws ServerInternalErrorException, SipTransactionException {
		super(method, localParty, ((FromHeader) serverTransaction.getRequest()
				.getHeader(FromHeader.NAME)).getAddress());
		this.serverTransaction = serverTransaction;
				
		// Only SIP Version = 2.0 supported
		if (!isSipVersion2()) {
			sendResponse(Response.VERSION_NOT_SUPPORTED, null);
			throw new SipTransactionException("Sip version not supported: " + serverTransaction.getRequest().getSIPVersion());
		}
		
		// Check if there is a sipcontext
		if ((dialog=serverTransaction.getDialog())!=null) {
			sipContext = (SipContext) dialog.getApplicationData();
		}
		
	}
	
	public ServerTransaction getServerTransaction(){
		return serverTransaction;
	}

	/**
	 * Generate a response outside a SIP dialog
	 * 
	 * @param code
	 * @param sdp
	 * @throws ServerInternalErrorException 
	 */
	public void sendResponse(int code, SessionDescription sdp) throws ServerInternalErrorException {
		
		Response response;
		try {
			response = UaFactory.getMessageFactory().createResponse(code,
					serverTransaction.getRequest());

			// Set user agent header
			if (UaFactory.getUserAgentHeader() != null) {
				response.setHeader(UaFactory.getUserAgentHeader());
			}

			// Set to tag
			if ( dialog != null &&  dialog.getLocalTag() != null){
				localTag = dialog.getLocalTag();
			}
			if (localTag == null){
				localTag = SipHeaderHelper.getNewRandomTag();
			}
			ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
			toHeader.setTag(localTag);


			// Set contact header
			if (localParty != null) {
				ContactHeader contactHeader = buildContactHeader();
				response.setHeader(contactHeader);
			}

			if (sdp != null) {
				ContentTypeHeader contentTypeHeader = UaFactory.getHeaderFactory()
						.createContentTypeHeader("application", "SDP");
				response.setContent(sdp, contentTypeHeader);
			}

			log.info("SIP response send\n"
					+ ">>>>>>> SIP response send >>>>>>>\n" + "\tTRANSACTION: "
					+ serverTransaction.getBranchId() + "\n" + "\t CODE: "
					+ response.getStatusCode() + "\n" + response.toString()
					+ "\n" 
					+ ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			serverTransaction.sendResponse(response);
			log.info("Transaction goes to state: " + serverTransaction.getState());

		} catch (Exception e) {
			log.error(
					"Found problems to build and send transaction response code: "	+ code, e);
			if (code == Response.SERVER_INTERNAL_ERROR) {
				throw new ServerInternalErrorException("Unable to send response code 500. GIVE UP!!!", e);
			} else {
				sendResponse(Response.SERVER_INTERNAL_ERROR, null);
			}
		}
	}

	private ContactHeader buildContactHeader() throws ParseException {
		return UaFactory.getHeaderFactory().createContactHeader(localParty.getContact());
	}
	
	protected int getContentLength (Request request) throws ServerInternalErrorException, SipTransactionException {
		//Check if invites provides a SDP
		ContentLength clHeader = (ContentLength) request.getHeader(ContentLength.NAME);
		if (clHeader == null) {
			sendResponse(Response.BAD_REQUEST, null);
			throw new SipTransactionException("Unable to find Content-length in Server Request");
		} else {
			return clHeader.getContentLength();
		}
	}

	private boolean isSipVersion2() {
		if (serverTransaction.getRequest().getSIPVersion().equals("SIP/2.0")) {
			return true;
		} else {
			return false;
		}
	}
	
//	@Override
//	protected void processTimeOut(TimeoutEvent event) {
//		log.warn("Timeout detected while processing Server Transaction");
//		
//	}
	// ////////////////////
	//
	// SDP Port Manager Interface
	//
	// ////////////////////

	@Override
	public void onEvent(SdpPortManagerEvent event) {
		// Remove this transaction as a listener of the SDP Port Manager
		event.getSource().removeListener(this);

		EventType eventType = event.getEventType();
		// List of events with default behavior in server transactions
		try {
			if (eventType != null) { // ok
				SessionSpec ss;
				try {
					ss = event.getMediaServerSdp();
					localSdp = SdpConversor.sessionSpec2SessionDescription(ss);
				} catch (SdpException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (SdpPortManagerEvent.ANSWER_GENERATED.equals(eventType)) {
					// Generated after processSdpOffer : SDP = response to give
					log.info("SDP answer successfully generated");
					this.sendResponse(Response.OK, localSdp);
				} else if (SdpPortManagerEvent.OFFER_GENERATED.equals(eventType)) {
					// Server INVITE with no SDP. Offer sent in 200ok response
					log.info("SDP offer successfully generated");
					this.sendResponse(Response.OK, localSdp);
				} else {
					log.error("Incorrect event received from SdpPortManager: " + eventType);
					this.sendResponse(Response.SERVER_INTERNAL_ERROR,null);
					sipContext.failedCall();
				}
			} else { // error
				MediaErr error = event.getError();

				if (SdpPortManagerEvent.RESOURCE_UNAVAILABLE.equals(error)) {
					log.warn("SDP failed. No resources available");
					this.sendResponse(Response.TEMPORARILY_UNAVAILABLE, null);
					sipContext.failedCall();
				} else if (SdpPortManagerEvent.SDP_NOT_ACCEPTABLE.equals(error)) {
					log.warn("SDP failed. Not accepeted");
					this.sendResponse(Response.UNSUPPORTED_MEDIA_TYPE, null);
					sipContext.failedCall();
				} else {
					log.error("Incorrect event received from SdpPortManager: " + eventType);
					this.sendResponse(Response.SERVER_INTERNAL_ERROR,null);
					sipContext.failedCall();
				}
			}
			//TODO in SdpPortManagerEventImpl 
			/*
			if (SdpPortManagerEvent.OFFER_PARTIALLY_ACCEPTED.equals(eventType)) {
				// Try to initiate the connection with partial media
				log.warn("SDP offer partially generated");
				this.sendResponse(Response.OK, localSdp);

			} else {
				log.error("Incorrect event received from SdpPortManager: " + eventType);
				this.sendResponse(Response.SERVER_INTERNAL_ERROR,null);
				sipContext.failedCall();
				
			}
			*/
		} catch (ServerInternalErrorException e) {
			log.error("Server error while managing SdpPortManagerEvent:"+ eventType, e);
			sipContext.failedCall();
		} finally {
			release();
		}
	}
	
}
