package com.kurento.commons.sip.transaction;

import javax.sip.ResponseEvent;
import javax.sip.message.Request;

import com.kurento.commons.sip.agent.SipContext;
import com.kurento.commons.sip.exception.ServerInternalErrorException;

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
