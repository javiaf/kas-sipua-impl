package com.kurento.commons.sip.agent;

import com.kurento.commons.sip.event.SipEvent;

public interface UaMessageListener {
	
	public void onEvent(SipEvent event);

}
