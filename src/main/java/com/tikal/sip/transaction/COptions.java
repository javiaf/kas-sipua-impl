/**
 * COptions class
 * Apr-2007
 * 
 */
package com.tikal.sip.transaction;

import javaxt.sip.ResponseEvent;
import javaxt.sip.address.Address;
import javaxt.sip.message.Request;

import com.tikal.sip.agent.SipEndPointImpl;
import com.tikal.sip.exception.ServerInternalErrorException;


public class COptions extends CTransaction {


	public COptions(SipEndPointImpl user, Address remoteParty) throws ServerInternalErrorException {
		super(Request.OPTIONS, user, remoteParty);

		// Add special headers for INVITE
		request.addHeader(buildContactHeader()); // Contact
		request.addHeader(buildAllowHeader()); // Allow
		request.addHeader(buildSupportedHeader()); // SupportHeader
		
		this.sendRequest(null);

	}

	@Override
	public void processResponse(ResponseEvent event){ 
		// Processing response
	}
	
}
