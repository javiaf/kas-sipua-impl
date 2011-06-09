package com.tikal.sip.transaction;

import javaxt.sip.ServerTransaction;
import javaxt.sip.TimeoutEvent;
import javaxt.sip.message.Request;
import javaxt.sip.message.Response;

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
