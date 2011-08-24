package com.kurento.commons.sip.transaction;

import javax.sip.ServerTransaction;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.exception.ServerInternalErrorException;
import com.kurento.commons.sip.exception.SipTransactionException;

public class SCancel extends STransaction {

	public SCancel(ServerTransaction serverTransaction,
			SipEndPointImpl localParty) throws ServerInternalErrorException,
			SipTransactionException {
		super(Request.CANCEL, serverTransaction, localParty);

		if (sipContext == null)
			sendResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST, null);
		else {
			sendResponse(Response.OK, null);
			sipContext.cancelCall();
		}
	}

}
