package com.kurento.commos.utils;

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
