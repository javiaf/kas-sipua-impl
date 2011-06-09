package com.tikal.sip.transaction;

import javaxt.sip.ResponseEvent;
import javaxt.sip.message.Request;

import com.tikal.sip.agent.SipContext;
import com.tikal.sip.exception.ServerInternalErrorException;

public class CBye extends CTransaction {

	public CBye(SipContext sipContext) throws ServerInternalErrorException {
		super(Request.BYE,  sipContext);
		sendRequest(null);
		sipContext.terminatedCall();
	}
	
	@Override
	public void processResponse(ResponseEvent event) {

	}

}
