package com.kurento.commons.sip.transaction;

import javax.sip.ResponseEvent;
import javax.sip.message.Request;

import com.kurento.commons.sip.agent.SipContext;
import com.kurento.commons.sip.exception.ServerInternalErrorException;

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
