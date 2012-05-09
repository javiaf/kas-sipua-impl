package com.kurento.commons.sip.agent;

import javax.sip.SipException;

import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.EndPointListener;
import com.kurento.commons.ua.UA;

public class EndPointFactory {

	
	public static EndPoint getInstance(String userName, String realm,
			String password, int expires, UA ua, EndPointListener handler) throws Exception {
		return getInstance(userName, realm, password,
				expires, ua, handler, true);
	}
	
	
	public static EndPoint getInstance(String userName, String realm,
			String password, int expires, UA ua, EndPointListener handler,
			Boolean receiveCall) throws Exception {
		if (!(ua instanceof UaImpl))
			throw new SipException("Incorrect UA");
		EndPoint endPoint = new SipEndPointImpl(userName, realm, password,
				expires, (UaImpl) ua, handler, receiveCall);
		return endPoint;
	}

}
