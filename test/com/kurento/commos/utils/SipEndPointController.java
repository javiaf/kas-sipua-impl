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
