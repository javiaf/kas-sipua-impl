package com.tikal.sip.agent;

import java.text.ParseException;
import java.util.ArrayList;

import javaxt.sip.PeerUnavailableException;
import javaxt.sip.SipFactory;
import javaxt.sip.address.AddressFactory;
import javaxt.sip.header.HeaderFactory;
import javaxt.sip.header.UserAgentHeader;
import javaxt.sip.message.MessageFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tikal.media.NetworkConnectionFactory;
import com.tikal.sip.UA;
import com.tikal.sip.util.SipConfig;


public class UaFactory {
	
	private static final Log log = LogFactory.getLog(UaFactory.class);
	
	private static final String USER_AGENT = "TikalUa/1.0.0";
	
	// UA initializations
	private static SipFactory sipFactory = SipFactory.getInstance();
	static {
		sipFactory.setPathName("com.tikal");//("gov.nist");
	}
	
	// Create Factory objects
	private static AddressFactory addressFactory;
	static {
		try {
			addressFactory = sipFactory.createAddressFactory();
		} catch (PeerUnavailableException e) {
			log.error("Address Factory initialization error",e);
		}
	}
	private static HeaderFactory headerFactory;
	static {
		try {
			headerFactory = sipFactory.createHeaderFactory();
		} catch (PeerUnavailableException e) {
			log.error("Header Factory initialization error",e);
		}
	}
	private static MessageFactory messageFactory;
	static {
			try {
				messageFactory = sipFactory.createMessageFactory();
			} catch (PeerUnavailableException e) {
				log.error("Message Factory initialization error",e);
			}
	}
	private static NetworkConnectionFactory networkConnectionFactory;

	private static UserAgentHeader userAgent;
	static {
		try {
			userAgent = UaFactory.getHeaderFactory().createUserAgentHeader(new ArrayList<String>() {
				private static final long serialVersionUID = 1L;
				{ add(USER_AGENT); }
			});
		} catch (ParseException e) {
			log.error("User Agent header initialization error",e);
		}
	}
	/////////
	
	public static UA getInstance (SipConfig config) throws Exception {
		UaImpl ua = new UaImpl(config);
		return ua;
	}
	
	public static UserAgentHeader getUserAgentHeader () {
		return userAgent;
	}
	
	public static SipFactory getSipFactory () {
		return sipFactory;
	}
	
	public static MessageFactory getMessageFactory() {
		return messageFactory;
	}

	public static HeaderFactory getHeaderFactory() {
		return headerFactory;
	}

	public static AddressFactory getAddressFactory() {
		return addressFactory;
	}

	public static void setNetworkConnectionFactory (NetworkConnectionFactory networkConnectionFactory) {
		UaFactory.networkConnectionFactory = networkConnectionFactory;
	}
	
	public static NetworkConnectionFactory getNetworkConnectionFactory(){
		return networkConnectionFactory;
	}
}