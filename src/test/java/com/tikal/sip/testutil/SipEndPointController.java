package com.tikal.sip.testutil;

import com.tikal.sip.SipEndPointListener;
import com.tikal.sip.event.SipEndPointEvent;
import com.tikal.sip.event.SipEventType;

public class SipEndPointController implements SipEndPointListener {
		
	private SipListener sipEndPointListener = new SipListener();
	
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
