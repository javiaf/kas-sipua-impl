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
