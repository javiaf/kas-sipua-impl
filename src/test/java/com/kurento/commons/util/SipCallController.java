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
package com.kurento.commons.util;

import com.kurento.commons.sip.SipCallListener;
import com.kurento.commons.sip.event.SipCallEvent;
import com.kurento.commons.sip.event.SipEventType;

public class SipCallController implements SipCallListener{

	private SipListener sipCallListener = new SipListener();
	private String name;
	public SipCallController(){}
	public SipCallController(String name) {
		this.name = name;
	}

	@Override
	public void onEvent(SipCallEvent event) {
		sipCallListener.onEvent(event);
	}
	
	public SipCallEvent pollSipEndPointEvent (int timeoutSec) throws InterruptedException {
		return sipCallListener.poll(timeoutSec);
	}

	public SipEventType pollSipEndPointEventType (int timeoutSec) throws InterruptedException {
		SipCallEvent event = sipCallListener.poll(timeoutSec);
		if (event != null) {
			return event.getEventType();
		} else {
			return null;
		}
	}
}
