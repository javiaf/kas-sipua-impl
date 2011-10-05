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
package com.kurento.commos.utils;

import com.kurento.commons.sip.SipEndPointListener;
import com.kurento.commons.sip.event.SipEndPointEvent;
import com.kurento.commons.sip.event.SipEventType;

public class SipEndPointController implements SipEndPointListener {
		
	private SipListener sipEndPointListener = new SipListener();
	private String name;
	public SipEndPointController(){};
	public SipEndPointController(String name) {
		this.name = name;
	}
	
	@Override
	public void onEvent(SipEndPointEvent event) {
		sipEndPointListener.onEvent(event);
	}
	
	public SipEndPointEvent pollSipEndPointEvent (int timeoutSec) throws InterruptedException {
		return sipEndPointListener.poll(timeoutSec);
	}
	
	public SipEventType pollSipEndPointEventType (int timeoutSec) throws InterruptedException {
		SipEndPointEvent event = sipEndPointListener.poll(timeoutSec);
		if (event != null) {
			return event.getEventType();
		} else {
			return null;
		}
	}
}
