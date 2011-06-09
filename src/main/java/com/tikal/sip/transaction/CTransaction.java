package com.tikal.sip.transaction;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javaxt.sip.ClientTransaction;
import javaxt.sip.DialogState;
import javaxt.sip.InvalidArgumentException;
import javaxt.sip.ResponseEvent;
import javaxt.sip.SipException;
import javaxt.sip.TransactionUnavailableException;
import javaxt.sip.address.Address;
import javaxt.sip.address.SipURI;
import javaxt.sip.header.AcceptHeader;
import javaxt.sip.header.AllowHeader;
import javaxt.sip.header.CSeqHeader;
import javaxt.sip.header.CallIdHeader;
import javaxt.sip.header.ContactHeader;
import javaxt.sip.header.ContentTypeHeader;
import javaxt.sip.header.ExpiresHeader;
import javaxt.sip.header.FromHeader;
import javaxt.sip.header.MaxForwardsHeader;
import javaxt.sip.header.SupportedHeader;
import javaxt.sip.header.ToHeader;
import javaxt.sip.header.UserAgentHeader;
import javaxt.sip.header.ViaHeader;
import javaxt.sip.message.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tikal.sip.agent.SipContext;
import com.tikal.sip.agent.SipEndPointImpl;
import com.tikal.sip.agent.UaFactory;
import com.tikal.sip.agent.UaImpl;
import com.tikal.sip.exception.ServerInternalErrorException;
import com.tikal.sip.util.SipHeaderHelper;

/**
 * 
 * @author fjlopez
 */
public abstract class CTransaction extends Transaction {

	protected ClientTransaction clientTransaction;
	protected Request request;

	// General attributes
	protected static Log log = LogFactory.getLog(CTransaction.class);

	// ////////////
	//
	// CONSTRUCTOR
	//
	// ////////////

	protected CTransaction(String method, SipEndPointImpl localParty,
			Address remoteParty) throws ServerInternalErrorException {
		super(method, localParty, remoteParty);
		createRequest();
	}

	/**
	 * This constructor is intended to create a transaction within a dialog.
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

	protected CallIdHeader buildCallIdHeader() {
		CallIdHeader callIdHeader;
		if (!(dialog != null && (callIdHeader = dialog.getCallId()) != null)) {
			callIdHeader = localParty.getUa().getSipProvider().getNewCallId();
		}
		return callIdHeader;
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
				ua.getLocalAddress(), ua.getLocalPort(), ua.getTransport(),
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

	protected ContactHeader buildContactHeader() {
		return UaFactory.getHeaderFactory().createContactHeader(
				localParty.getContact());
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

		if (sdp != null) {
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
					"Sip Exception sending INVITE request", e);
		}
	}

	public abstract void processResponse(ResponseEvent event)
			throws ServerInternalErrorException;

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
		// List of events with default behavor in client transactions
		try {
			if (eventType != null) { // ok
				if (SdpPortManagerEvent.OFFER_GENERATED.equals(eventType)) {
					// Generated after processSdpOffer : SDP = response to give
					this.sendRequest(event.getMediaServerSdp());

				} else if (SdpPortManagerEvent.ANSWER_PROCESSED.equals(eventType)) {
					log.debug("SdpPortManager successfully processed SDP answer received from remote peer");
					// sipContext.notifySipCallEvent(SipCallEvent.)
				} else {
					log.error("Unknown event received from SdpPortManager: "
							+ eventType);
					// sipContext.notifySipCallEvent(SipCallEvent.SERVER_INTERNAL_ERROR);
				}
			} else { // error
				MediaErr error = event.getError();

				if (SdpPortManagerEvent.RESOURCE_UNAVAILABLE.equals(error)) {
					// Notify error
					log.error("No media resources to attend client transaction:"
							+ clientTransaction.toString());
					// sipContext.notifySipCallEvent(SipCallEvent.MEDIA_RESOURCE_NOT_AVAILABLE);
				} else if (SdpPortManagerEvent.SDP_NOT_ACCEPTABLE.equals(error)) {
					log.error("SDP not acceptable in client transaction: "
							+ clientTransaction.toString());
					// sipContext.notifySipCallEvent(SipCallEvent.MEDIA_NOT_SUPPORTED);
				} else {
					log.error("Unknown event received from SdpPortManager: "
							+ error);
					// sipContext.notifySipCallEvent(SipCallEvent.SERVER_INTERNAL_ERROR);
				}
			}

		} catch (ServerInternalErrorException e) {
			log.error("Server error while managing SdpPortManagerEvent:"
					+ eventType, e);
			// sipContext.notifySipCallEvent(SipCallEvent.SERVER_INTERNAL_ERROR);
		} finally {
			// Release media resources managed by this transaction
			release();
		}
	}

	// public void processTimeOut(TimeoutEvent timeoutEvent) {
	// // Nothing general to do.
	// // This function avoids every transaction to define processTimeOut
	// log.error("Time Out while waiting a response from client transaction: " +
	// method);
	// release();
	// }

}
