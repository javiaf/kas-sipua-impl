package com.tikal.sip.transaction;

import javax.sip.ResponseEvent;
import javax.sip.message.Request;

import com.tikal.sip.agent.SipContext;
import com.tikal.sip.exception.ServerInternalErrorException;

public class CCancel extends CTransaction {

	public CCancel(Request cancelRequest, SipContext sipContext)
			throws ServerInternalErrorException {
		super(Request.CANCEL, cancelRequest, sipContext.getEndPoint(),
				sipContext.getRemoteParty());
		this.sendCancel();
	}

	@Override
	public void processResponse(ResponseEvent event)
			throws ServerInternalErrorException {

	}

}
