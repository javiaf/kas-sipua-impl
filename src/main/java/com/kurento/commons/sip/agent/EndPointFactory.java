package com.kurento.commons.sip.agent;

import javax.sip.SipException;

import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.EndPointListener;
import com.kurento.commons.ua.UA;
import com.kurento.commons.ua.timer.KurentoUaTimer;

public class EndPointFactory {

	
	public static EndPoint getInstance(String userName, String realm,
			String password, int expires, UA ua, EndPointListener handler,
			KurentoUaTimer timer) throws Exception {
		return getInstance(userName, realm, password,
				expires, ua, handler, timer, true);
	}
	
	
	public static EndPoint getInstance(String userName, String realm,
			String password, int expires, UA ua, EndPointListener handler,
			KurentoUaTimer timer, Boolean receiveCall) throws Exception {
		if (!(ua instanceof UaImpl))
			throw new SipException("Incorrect UA");
		EndPoint endPoint = new SipEndPointImpl(userName, realm, password,
				expires, (UaImpl) ua, handler, timer, receiveCall);
		return endPoint;
	}

}
