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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.ResponseEvent;
import javax.sip.TimeoutEvent;
import javax.sip.address.URI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.event.SipEndPointEvent;
import com.kurento.commons.sip.exception.ServerInternalErrorException;

public class CRegister extends CTransaction {

	public CRegister(SipEndPointImpl localParty)
			throws ServerInternalErrorException {
		super(Request.REGISTER, localParty, localParty.getAddress());

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
	protected CSeqHeader buildCSeqHeader() throws ParseException,
			InvalidArgumentException {
		return UaFactory.getHeaderFactory().createCSeqHeader(
				localParty.getcSeqNumber(), method);
	}

	@Override
	public void processResponse(ResponseEvent event)
			throws ServerInternalErrorException {
		Response response = event.getResponse();
		int statusCode = response.getStatusCode();

		if (statusCode == Response.OK) {
			log.info("<<<<<<< 200 OK: Register sucessfull for user: "
					+ localParty.getAddress());
			localParty.notifyEvent(SipEndPointEvent.REGISTER_USER_SUCESSFUL);
		} else if (statusCode == Response.UNAUTHORIZED
				|| statusCode == Response.PROXY_AUTHENTICATION_REQUIRED) { // Peer
																			// Authentication
			sendWithAuth(event);
		} else if (statusCode == Response.REQUEST_TIMEOUT) { // 408: Request
																// TimeOut
			log.warn("<<<<<<< 408 REQUEST_TIMEOUT: Register Failure. Unable to contact registrar at "
					+ localParty.getAddress());
			localParty.notifyEvent(SipEndPointEvent.REGISTER_USER_FAIL);
		} else if (statusCode == Response.NOT_FOUND) { // 404: Not Found
			log.warn("<<<<<<< 404 NOT_FOUND: Register Failure. User "
					+ localParty.getAddress() + " not found");
			localParty.notifyEvent(SipEndPointEvent.REGISTER_USER_NOT_FOUND);
		} else if (statusCode == Response.FORBIDDEN) { // Forbidden
			log.warn("<<<<<<< 403 FORBIDDEN: Register Failure. User "
					+ localParty.getAddress() + " forbiden");
			localParty.notifyEvent(SipEndPointEvent.REGISTER_USER_FAIL);
		} else if (statusCode == Response.SERVER_INTERNAL_ERROR) { // server
																	// errors
			log.warn("<<<<<<< 500 SERVER_INTERNAL_ERROR Register: Server Error: "
					+ response.getStatusCode());
			localParty.notifyEvent(SipEndPointEvent.SERVER_INTERNAL_ERROR);
		} else if (statusCode == Response.SERVICE_UNAVAILABLE) { // server
																	// errors
			log.warn("<<<<<<< 503 SERVICE_UNAVAILABLE Register: Service unavailable: "
					+ response.getStatusCode());
			localParty.notifyEvent(SipEndPointEvent.SERVER_INTERNAL_ERROR);
		} else { // Non supported response code Discard
			log.warn("Register Failure. Status code: "
					+ response.getStatusCode());
			localParty.notifyEvent(SipEndPointEvent.SERVER_INTERNAL_ERROR);
		}
	}

	private void sendWithAuth(ResponseEvent event)
			throws ServerInternalErrorException {
		Response response = event.getResponse();
		int statusCode = response.getStatusCode();

		createRequest();

		if (statusCode == 401 || statusCode == 407) { // 401 Peer Authentication
			log.info("Authentication Required in REGISTER transaction for user: "
					+ localParty.getAddress());
			AuthorizationHeader authorization = getAuthorizationHearder(response);
			request.setHeader(authorization);
		} else if (statusCode == 407) { // 407: Proxy Auth Required
			log.info("Proxy Authentication Required in REGISTER transaction for user: "
					+ localParty.getAddress());
			ProxyAuthenticateHeader proxyAuthenticateHeader;
			proxyAuthenticateHeader = (ProxyAuthenticateHeader) response
					.getHeader(ProxyAuthenticateHeader.NAME);
			log.debug("ProxyAuthenticateHeader is "
					+ proxyAuthenticateHeader.toString());
			request.setHeader(proxyAuthenticateHeader);
		}

		sendRequest(null); // 2nd request with authentication

	}

	//AuthenticationHearder RFC2617 construction for request digest without	qop parameter and MD5 hash algorithm.
	private AuthorizationHeader getAuthorizationHearder(Response response) throws ServerInternalErrorException{
		WWWAuthenticateHeader wwwAuthenticateHeader;
		AuthorizationHeader authorization = null;
		try {
			wwwAuthenticateHeader = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
	
			String schema = wwwAuthenticateHeader.getScheme();
			String realm = wwwAuthenticateHeader.getRealm();
			String nonce = wwwAuthenticateHeader.getNonce();
			String alg = wwwAuthenticateHeader.getAlgorithm();
			String opaque = wwwAuthenticateHeader.getOpaque();
	
			log.debug("WWWAuthenticateHeader is "+ wwwAuthenticateHeader.toString());
			
		
			URI localUri = localParty.getAddress().getURI();
			String user = localParty.getUserName();
			
			HeaderFactory factory = UaFactory.getSipFactory()
					.createHeaderFactory();
			authorization = factory.createAuthorizationHeader(schema);
			authorization.setUsername(user);
			authorization.setRealm(realm);
			authorization.setNonce(nonce);
			authorization.setURI(localUri);
			String respon = getAuthResponse(user, realm,
					localParty.getPassword(),
					this.method,localUri.toString(), nonce, alg);
			authorization.setResponse(respon);
			authorization.setAlgorithm(alg);
			authorization.setOpaque(opaque);

		} catch (ParseException e1) {
			log.error(e1);
			throw new ServerInternalErrorException("Error generating authentication hearder", e1);
		} catch (PeerUnavailableException e) {
			log.error(e);
			throw new ServerInternalErrorException("Error generating authentication hearder", e);
		}
		return authorization;

	}

	@Override
	public void processTimeOut(TimeoutEvent timeoutEvent) {
		log.error("Register Failure due to request timeout");
		localParty.notifyEvent(SipEndPointEvent.REGISTER_USER_FAIL);
	}

	private String getAuthResponse(String userName, String realm,
			String password, String method, String uri, String nonce, String algorithm) {
		String cnonce = null;
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			log.error(e);
		}
		// A1
		String A1 = userName + ":" + realm + ":" + password;
		byte mdbytes[] = messageDigest.digest(A1.getBytes());
		String HA1 = toHexString(mdbytes);
		log.debug("DigestClientAuthenticationMethod for HA1:"
				+ HA1 + "!");
		// A2
		String A2 = method.toUpperCase() + ":" + uri;
		mdbytes = messageDigest.digest(A2.getBytes());
		String HA2 = toHexString(mdbytes);
		log.debug("DigestClientAuthenticationMethod for HA2:"
				+ HA2 + "!");
		// KD
		String KD = HA1 +":" + nonce;
		// if (cnonce != null) {
		// if(cnonce.length()>0) KD += ":" + cnonce;
		// }
		KD += ":" + HA2;
		mdbytes = messageDigest.digest(KD.getBytes());
		String response = toHexString(mdbytes);

		log.debug("DigestClientAlgorithm, response generated: " + response);

		return response;

	}

	public static String toHexString(byte b[]) {
		int pos = 0;
		char[] c = new char[b.length * 2];
		for (int i = 0; i < b.length; i++) {
			c[pos++] = toHex[(b[i] >> 4) & 0x0F];
			c[pos++] = toHex[b[i] & 0x0f];
		}
		return new String(c);
	}

	private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

}
