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
import java.util.ArrayList;
import java.util.List;

import javax.sdp.SdpException;
import javax.sip.ClientTransaction;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.AcceptHeader;
import javax.sip.header.AllowHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.SupportedHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.media.format.conversor.SdpConversor;
import com.kurento.commons.mscontrol.EventType;
import com.kurento.commons.mscontrol.MediaErr;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.sip.agent.SipContext;
import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.agent.UaImpl;
import com.kurento.commons.sip.util.SipHeaderHelper;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

/**
 * 
 * @author fjlopez
 */
public abstract class CTransaction extends Transaction {

	protected ClientTransaction clientTransaction;
	protected Request request;

	// General attributes
	protected static Logger log = LoggerFactory.getLogger(CTransaction.class);

	// ////////////
	//
	// CONSTRUCTOR
	//
	// ////////////

	/**
	 * Allows creation of out of dialog Client Transactions
	 * 
	 * @param method
	 * @param localParty
	 * @param remoteParty
	 * @throws ServerInternalErrorException
	 */
	protected CTransaction(String method, SipEndPointImpl localParty,
			Address remoteParty) throws ServerInternalErrorException {
		super(method, localParty, remoteParty);
		createRequest();
	}

	/**
	 * This constructor is intended to create a Client Transaction within a
	 * dialog.
	 * 
	 * @param method
	 * @param dialog
	 * @throws ServerInternalErrorException
	 */
	protected CTransaction(String method, SipContext sipContext)
			throws ServerInternalErrorException {
		super(method, sipContext.getEndPoint(), sipContext.getRemoteParty());
		dialog = sipContext.getDialog();
		createRequest();
	}

	/**
	 * This constructor is intented to create a Client Transaction when request
	 * is already available (only for cancel requests)
	 * 
	 * @param method
	 * @param request
	 * @param localParty
	 * @param remoteParty
	 * @throws ServerInternalErrorException
	 */
	protected CTransaction(String method, Request request,
			SipEndPointImpl localParty, Address remoteParty)
			throws ServerInternalErrorException {
		super(method, localParty, remoteParty);
		this.request = request;
		setClientTransaction();
	}

	// //////////////
	//
	// GETTERS
	//
	// //////////////
	public ClientTransaction getClientTransaction() {
		return clientTransaction;
	}

	// //////////////
	//
	// BUILD REQUEST
	//
	// //////////////

	protected void createRequest() throws ServerInternalErrorException {
		// Check if dialog exists
		if (dialog == null) {
			try {
				CallIdHeader callIdHeader = buildCallIdHeader();
				FromHeader fromHeader = buildFromHeader();
				ToHeader toHeader = buildToHeader();
				List<ViaHeader> viaHeaders = buildViaHeaders();
				CSeqHeader cSeqHeader = buildCSeqHeader();
				MaxForwardsHeader maxForwardsHeader = buildMaxForwardsHeader();
				UserAgentHeader userAgentHeader = buildUserAgentHeader();
				ContactHeader contactHeader = buildContactHeader();

				// AllowHeader allowHeader = buildAllowHeader();
				// SupportedHeader supportedHeader = buildSupportedHeader();
				// AcceptHeader acceptHeader = buildAcceptHeader();
				// ExpiresHeader expiresHeader = buildExpiresHeader();
				//
				// RequireHeader requireHeader;

				SipURI requestURI = buildRequestURI();

				// Create Request
				request = UaFactory.getMessageFactory().createRequest(
						requestURI, method, callIdHeader, cSeqHeader,
						fromHeader, toHeader, viaHeaders, maxForwardsHeader);
				request.addHeader(userAgentHeader);
				request.addHeader(contactHeader);
			} catch (ParseException e) {
				throw new ServerInternalErrorException(
						"Parse error while creating SIP client transaction", e);
			} catch (InvalidArgumentException e) {
				throw new ServerInternalErrorException(
						"Invalid argument for SIP client transaction", e);
			}
		} else {
			try {
				// Create Request from dialog
				request = dialog.createRequest(method);
			} catch (SipException e) {
				throw new ServerInternalErrorException(
						"Dialog is not yet established", e);
			}
		}
		setClientTransaction();
	}

	private void setClientTransaction() throws ServerInternalErrorException {
		// Create client transaction
		try {
			clientTransaction = localParty.getUa().getSipProvider()
					.getNewClientTransaction(request);
			clientTransaction.setApplicationData(this);

			// Check if after request a dialog should be created
			dialog = clientTransaction.getDialog();
		} catch (TransactionUnavailableException e) {
			throw new ServerInternalErrorException(
					"Transaction error while creating new client transaction",
					e);
		}
	}

	protected CallIdHeader buildCallIdHeader()
			throws ServerInternalErrorException {
		try {
			CallIdHeader callIdHeader;
			if (!(dialog != null && (callIdHeader = dialog.getCallId()) != null)) {
				if (localParty.getUa() == null
						|| localParty.getUa().getSipProvider() != null) {
					callIdHeader = localParty.getUa().getSipProvider()
							.getNewCallId();
				} else {
					throw new ServerInternalErrorException(
							"User Agent not initialized.");
				}
			}
			return callIdHeader;
		} catch (Exception e) {
			throw new ServerInternalErrorException("Error building hearder.", e);
		}
	}

	protected FromHeader buildFromHeader() throws ParseException {
		if (dialog != null && dialog.getLocalTag() != null) {
			localTag = dialog.getLocalTag();
		}
		if (localTag == null) {
			localTag = SipHeaderHelper.getNewRandomTag();
		}
		return UaFactory.getHeaderFactory().createFromHeader(
				localParty.getAddress(), localTag);
	}

	protected ToHeader buildToHeader() throws ParseException {
		return UaFactory.getHeaderFactory().createToHeader(remoteParty, null);
	}

	protected List<ViaHeader> buildViaHeaders() throws ParseException,
			InvalidArgumentException {
		List<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		UaImpl ua = localParty.getUa();
		ViaHeader viaHeader = UaFactory.getHeaderFactory().createViaHeader(
				ua.getPublicAddress(), ua.getPublicPort(), ua.getTransport(),
				SipHeaderHelper.getNewRandomBranch());

		// add via headers
		viaHeaders.add(viaHeader);
		return viaHeaders;
	}

	protected CSeqHeader buildCSeqHeader() throws ParseException,
			InvalidArgumentException {
		Long cSeqNumber;
		if (!(dialog != null && (cSeqNumber = dialog.getLocalSeqNumber()) != 0)) {
			cSeqNumber = CTransaction.cSeqNumber;
		}
		return UaFactory.getHeaderFactory()
				.createCSeqHeader(cSeqNumber, method);
	}

	protected MaxForwardsHeader buildMaxForwardsHeader()
			throws InvalidArgumentException {
		return UaFactory.getHeaderFactory().createMaxForwardsHeader(
				localParty.getUa().getMaxForwards());
	}

	protected AllowHeader buildAllowHeader()
			throws ServerInternalErrorException {
		try {
			return UaFactory.getHeaderFactory().createAllowHeader(
					"INVITE,ACK,CANCEL,BYE");
		} catch (ParseException e) {
			throw new ServerInternalErrorException(
					"Parse Exception building Header Factory", e);
		}
	}

	protected SupportedHeader buildSupportedHeader()
			throws ServerInternalErrorException {
		try {
			return UaFactory.getHeaderFactory().createSupportedHeader("100rel");
		} catch (ParseException e) {
			throw new ServerInternalErrorException(
					"Parse Exception building Support header", e);
		}

	}

	protected ContentTypeHeader buildContentTypeHeader()
			throws ServerInternalErrorException {
		try {
			return UaFactory.getHeaderFactory().createContentTypeHeader(
					"application", "sdp");
		} catch (ParseException e) {
			throw new ServerInternalErrorException(
					"ParseException building contentType header", e);
		}
	}

	protected ContactHeader buildContactHeader() throws ServerInternalErrorException {
		ContactHeader contact = UaFactory.getHeaderFactory().createContactHeader(
				localParty.getContact());
//		try {
//			// Params from RFC5626
//			contact.setParameter("+sip.instance", "\"<urn:uuid:"+ localParty.getUa().getInstanceId()+">\"");
//			contact.setParameter("reg-id",String.valueOf(localParty.getUa().getRegId()));
//		} catch (ParseException e) {
//			throw new ServerInternalErrorException(
//					"ParseException building contact header", e);
//		}		
		return contact;
	}

	protected AcceptHeader buildAcceptHeader() throws ParseException {
		return UaFactory.getHeaderFactory().createAcceptHeader("application",
				"sdp");
	}

	protected ExpiresHeader buildExpiresHeader()
			throws ServerInternalErrorException {
		try {
			return UaFactory.getHeaderFactory().createExpiresHeader(
					localParty.getExpires());
		} catch (InvalidArgumentException e) {
			throw new ServerInternalErrorException(
					"Invalid argument building expires header", e);
		}
	}

	protected UserAgentHeader buildUserAgentHeader() throws ParseException {
		return UaFactory.getUserAgentHeader();

	}

	protected SipURI buildRequestURI() throws ParseException {
		return (SipURI) remoteParty.getURI();
	}

	// ///////////////////
	//
	// MANAGE TRANSACTION
	//
	// ///////////////////

	public void sendRequest(byte[] sdp) throws ServerInternalErrorException {

		if (sdp != null && sdp.length > 0) {
			try {
				request.setContent(sdp, buildContentTypeHeader());
			} catch (ParseException e) {
				throw new ServerInternalErrorException(
						"ParseException adding content to INVITE request:\n"
								+ sdp.toString(), e);
			}
		}
		log.info("SIP send request\n"
				+ ">>>>>>>>>> SIP send request >>>>>>>>>>\n"
				+ clientTransaction.getRequest().toString()
				+ ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		try {
			if (dialog != null
					&& DialogState.CONFIRMED.equals(dialog.getState()))
				dialog.sendRequest(clientTransaction);
			else
				clientTransaction.sendRequest();
		} catch (SipException e) {
			throw new ServerInternalErrorException(
					"Sip Exception sending  request", e); // BYE too...
		}
	}

	public abstract void processResponse(ResponseEvent event);
	
	public void processTimeout(){}



}
