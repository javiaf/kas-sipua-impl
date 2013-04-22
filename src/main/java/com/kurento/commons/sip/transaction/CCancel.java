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

import com.kurento.kas.sip.ua.KurentoSipException;

public class CCancel extends CTransaction {

	public CCancel(Request cancelRequest) throws KurentoSipException {
		// Cancel transaction requires external request calculated from
		// ClientTransaction being cancelled
		this.request = cancelRequest;
		// Send request with no SDP
		sendRequest(null);
	}

	@Override
	public void processResponse(ResponseEvent event) {
	}

}
