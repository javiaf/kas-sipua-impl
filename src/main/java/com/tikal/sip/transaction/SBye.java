package com.tikal.sip.transaction;

import javax.sip.ServerTransaction;
import javax.sip.TimeoutEvent;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.tikal.sip.agent.SipEndPointImpl;
import com.tikal.sip.exception.ServerInternalErrorException;
import com.tikal.sip.exception.SipTransactionException;

public class SBye extends STransaction {

	public SBye(ServerTransaction serverTransaction, SipEndPointImpl localParty)
			throws ServerInternalErrorException, SipTransactionException {
		super(Request.BYE, serverTransaction, localParty);

		if (sipContext == null)
			sendResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST, null);
		else {
			sendResponse(Response.OK, null);
			sipContext.terminatedCall();
		}
	}

	@Override
	public void processTimeOut(TimeoutEvent timeoutEvent) {
		// TODO Auto-generated method stub

	}

}
