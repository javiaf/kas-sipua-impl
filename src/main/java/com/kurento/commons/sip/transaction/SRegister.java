/*
Kurento Sip User Agent implementation.
Copyright (C) <2011>  <Tikal Technologies>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License version 3
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.kurento.commons.sip.transaction;

import javax.sip.ServerTransaction;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.exception.SipTransactionException;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public class SRegister extends STransaction {

	public SRegister(ServerTransaction serverTransaction) throws ServerInternalErrorException,
			SipTransactionException {
		super(Request.REGISTER, serverTransaction, null);
		
		sendResponse(Response.OK, null);

//		// TODO:This service is intended for 3rd Party registration. it will
//		// require a register interface to store register requests
//
//		// By now lets accept all requests
//		Response response;
//		try {
//			response = UaFactory.getMessageFactory().createResponse(Response.OK,
//					serverTransaction.getRequest());
//			serverTransaction.sendResponse(response);
//		} catch (Exception e) {
//			log.error("Parse exception receiving REGISTER request",e);
//		}
	}
}
