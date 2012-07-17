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
package com.kurento.commons.sip.testutils;

import com.kurento.ua.commons.EndPointEvent;
import com.kurento.ua.commons.EndPointListener;

public class SipEndPointController implements EndPointListener {

	private EventListener sipEndPointListener;

	public SipEndPointController(String id) {
		sipEndPointListener = new EventListener("EndPoint-" + id);
	}

	@Override
	public void onEvent(EndPointEvent event) {
		sipEndPointListener.onEvent(event);
	}

	public EndPointEvent pollSipEndPointEvent(int timeoutSec)
			throws InterruptedException {
		return sipEndPointListener.poll(timeoutSec);
	}

}
