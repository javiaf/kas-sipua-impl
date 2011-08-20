package com.tikal.sip.transaction;

import javax.sip.ResponseEvent;
import javax.sip.message.Request;

import com.tikal.sip.agent.SipContext;
import com.tikal.sip.exception.ServerInternalErrorException;

public class CCancel extends CTransaction {

	public CCancel(SipContext sipContext) throws ServerInternalErrorException {
		super(Request.CANCEL,  sipContext);
		this.sendCancel();
		
	}

	@Override
	public void processResponse(ResponseEvent event) {

	}

}
