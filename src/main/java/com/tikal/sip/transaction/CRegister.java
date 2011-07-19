package com.tikal.sip.transaction;

import java.text.ParseException;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.TimeoutEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.tikal.sip.agent.SipEndPointImpl;
import com.tikal.sip.agent.UaFactory;
import com.tikal.sip.event.SipEndPointEvent;
import com.tikal.sip.exception.ServerInternalErrorException;

public class CRegister extends CTransaction {

	public CRegister(SipEndPointImpl localParty) throws ServerInternalErrorException {
		super(Request.REGISTER,localParty,localParty.getAddress());

		// REGISTER send special request URI: RFC3261 , 10.2
		request.setRequestURI(localParty.getAddress().getURI());

		// Set REGISTER CallId according to RFC3261 , 10.2 - CSeq
		CallIdHeader registrarCallId;
		if ((registrarCallId = localParty.getRegistrarCallId()) != null) {
			request.setHeader(registrarCallId);
		} else {
			localParty.setRegistrarCallId((CallIdHeader) request
					.getHeader(CallIdHeader.NAME));
		}

		// Add specific REGISTER headers RFC3261, 10.2 - RequestURI
		request.addHeader(buildExpiresHeader());

	}
	
	// Override CSeqHeader according to RFC3261, 10.2 - CSeq
	protected CSeqHeader buildCSeqHeader() throws ParseException, InvalidArgumentException {
		return UaFactory.getHeaderFactory().createCSeqHeader(localParty.getcSeqNumber(), method);
	}

	@Override
	public void processResponse(ResponseEvent event) throws ServerInternalErrorException {
		Response response = event.getResponse();
		int statusCode = response.getStatusCode();

		if (statusCode == Response.OK) {
			log.info("<<<<<<< 200 OK: Register sucessfull for user: " + localParty.getAddress());
			localParty.notifyEvent(SipEndPointEvent.REGISTER_USER_SUCESSFUL);
		} else if (statusCode == Response.UNAUTHORIZED || statusCode == Response.PROXY_AUTHENTICATION_REQUIRED) { // Peer Authentication
			sendWithAuth (event);
		} else if (statusCode == Response.REQUEST_TIMEOUT) { // 408: Request TimeOut
			log.warn("<<<<<<< 408 REQUEST_TIMEOUT: Register Failure. Unable to contact registrar at " + localParty.getAddress());
			localParty.notifyEvent(SipEndPointEvent.REGISTER_USER_FAIL);
		} else if (statusCode == Response.NOT_FOUND ) { // 404: Not Found
			log.warn("<<<<<<< 404 NOT_FOUND: Register Failure. User " + localParty.getAddress() + " not found");
			localParty.notifyEvent(SipEndPointEvent.REGISTER_USER_NOT_FOUND);
		} else if (statusCode == Response.FORBIDDEN ) { // Forbidden
			log.warn("<<<<<<< 403 FORBIDDEN: Register Failure. User " + localParty.getAddress() + " forbiden");
			localParty.notifyEvent(SipEndPointEvent.REGISTER_USER_FAIL);
		} else if (statusCode == Response.SERVER_INTERNAL_ERROR ) { // server errors
			log.warn("<<<<<<< 500 SERVER_INTERNAL_ERROR Register: Server Error: " + response.getStatusCode());
			localParty.notifyEvent(SipEndPointEvent.SERVER_INTERNAL_ERROR);
		}else if (statusCode == Response.SERVICE_UNAVAILABLE) { // server errors
			log.warn("<<<<<<< 503 SERVICE_UNAVAILABLE Register: Service unavailable: " + response.getStatusCode());
			localParty.notifyEvent(SipEndPointEvent.SERVER_INTERNAL_ERROR);
		}else { // Non supported response code Discard
			log.warn("Register Failure. Status code: " + response.getStatusCode());
			localParty.notifyEvent(SipEndPointEvent.SERVER_INTERNAL_ERROR);
		}
	}
	
	private void sendWithAuth(ResponseEvent event) throws ServerInternalErrorException{
		Response response = event.getResponse();
		int statusCode = response.getStatusCode();

		createRequest();

		if (statusCode == 401 || statusCode == 407) { // 401 Peer Authentication
			log.info("Authentication Required in REGISTER transaction for user: " + localParty.getAddress());
			WWWAuthenticateHeader wwwAuthenticateHeader;
			wwwAuthenticateHeader = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
			log.debug("WWWAuthenticateHeader is " + wwwAuthenticateHeader.toString());
			request.setHeader(wwwAuthenticateHeader);
		} else if (statusCode == 407) { // 407: Proxy Auth Required
			log.info("Proxy Authentication Required in REGISTER transaction for user: " + localParty.getAddress());
			ProxyAuthenticateHeader proxyAuthenticateHeader;
			proxyAuthenticateHeader = (ProxyAuthenticateHeader) response.getHeader(ProxyAuthenticateHeader.NAME);
			log.debug("ProxyAuthenticateHeader is " + proxyAuthenticateHeader.toString());
			request.setHeader(proxyAuthenticateHeader);
		}
		
		sendRequest(null); // 2nd request with autentication

	}

	@Override
	public void processTimeOut(TimeoutEvent timeoutEvent) {
		log.error("Register Failure due to request timeout");
		localParty.notifyEvent(SipEndPointEvent.REGISTER_USER_FAIL);
	}
}
