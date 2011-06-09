package com.tikal.sip.testutil;

import com.tikal.sip.SipCallListener;
import com.tikal.sip.event.SipCallEvent;
import com.tikal.sip.event.SipEndPointEvent;
import com.tikal.sip.event.SipEventType;

public class SipCallController implements SipCallListener{

	private SipListener sipCallListener = new SipListener();

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
