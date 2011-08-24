/**
 * COptions class
 * Apr-2007
 * 
 */
package com.kurento.commons.sip.transaction;

import javax.sip.ResponseEvent;
import javax.sip.address.Address;
import javax.sip.message.Request;

import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.exception.ServerInternalErrorException;

public class COptions extends CTransaction {

	public COptions(SipEndPointImpl user, Address remoteParty)
			throws ServerInternalErrorException {
		super(Request.OPTIONS, user, remoteParty);

		// Add special headers for INVITE
		request.addHeader(buildContactHeader()); // Contact
		request.addHeader(buildAllowHeader()); // Allow
		request.addHeader(buildSupportedHeader()); // SupportHeader

		this.sendRequest(null);

	}

	@Override
	public void processResponse(ResponseEvent event) {
		// Processing response
	}

}
