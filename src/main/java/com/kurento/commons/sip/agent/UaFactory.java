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
package com.kurento.commons.sip.agent;

import java.text.ParseException;
import java.util.ArrayList;

import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.MessageFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commons.ua.UA;
import com.kurento.mscontrol.commons.MediaSession;

/**
 * The UaFactory is a singleton class able to create instances of a JAIN-SIP
 * User Agent implementation. It also wraps several JAIN-SIP factories required
 * to encode and decode SIP messages
 * <p>
 * Every time getInstance() method is called a new User Agent is created and
 * binded to ports according to SipConfig specification. Special care have to be
 * taken when multiple agents are instantiated as each one instantiate its own
 * SipStack and binds to its own socket, that can no be shared
 * <p>
 * Media configuration has to be set before User agent instantiation, as
 * MediaSession parameter can not be changed after instantiation. A change in
 * media capabilities requires a User Agent initialization.
 * 
 * @author Kurento
 * 
 */
public class UaFactory {

	private static final Logger log = LoggerFactory.getLogger(UaFactory.class);

	private static final String USER_AGENT = "KurentoUa/1.0.0";

	// UA initializations
	private static SipFactory sipFactory = SipFactory.getInstance();

	// Create Factory objects
	private static AddressFactory addressFactory;
	static {
		try {
			addressFactory = sipFactory.createAddressFactory();
		} catch (PeerUnavailableException e) {
			log.error("Address Factory initialization error", e);
		}
	}
	private static HeaderFactory headerFactory;
	static {
		try {
			headerFactory = sipFactory.createHeaderFactory();
		} catch (PeerUnavailableException e) {
			log.error("Header Factory initialization error", e);
		}
	}
	private static MessageFactory messageFactory;
	static {
		try {
			messageFactory = sipFactory.createMessageFactory();
		} catch (PeerUnavailableException e) {
			log.error("Message Factory initialization error", e);
		}
	}
	private static MediaSession mediaSession;

	private static UserAgentHeader userAgent;
	static {
		try {
			userAgent = UaFactory.getHeaderFactory().createUserAgentHeader(
					new ArrayList<String>() {
						private static final long serialVersionUID = 1L;
						{
							add(USER_AGENT);
						}
					});
		} catch (ParseException e) {
			log.error("User Agent header initialization error", e);
		}
	}

	// ///////

	/**
	 * Creates a new UA with the provided SIP configuration and based in the
	 * media information stored into the MediaSession object, that must be set
	 * before this method is called
	 */
	public static UA getInstance(SipConfig config) {
//		if (UaFactory.context == null) {
//			throw new Exception("Android context not setted.");
//		}
		UaImpl ua = new UaImpl(config);
		return ua;
	}
	
	public static NetworkListener getNetworkListener (UA ua) {
		if (ua instanceof UaImpl){
			return ((UaImpl) ua).getNetworkListener();
		} else {
			return null;
		}
	}

	/**
	 * Returns the User Agent header as defined by JAIN-SIP
	 * 
	 * @return User Agent Header
	 */
	public static UserAgentHeader getUserAgentHeader() {
		return userAgent;
	}

	/**
	 * Returns the JAIN-SIP SipFactory object that holds the SIP stack this User
	 * Agent utilizes for message interchange
	 * 
	 * @return SIP factory
	 */
	public static SipFactory getSipFactory() {
		return sipFactory;
	}

	/**
	 * Returns the JAIN-SIP MessageFactory object used by this User Agent to
	 * build SIP messages
	 * 
	 * @return SIP message factory
	 */
	public static MessageFactory getMessageFactory() {
		return messageFactory;
	}

	/**
	 * Returns the JAIN-SIP HeaderFactory object used by this User Agent to
	 * build SIP headers
	 * 
	 * @return SIP header factory
	 */
	public static HeaderFactory getHeaderFactory() {
		return headerFactory;
	}

	/**
	 * Returns the JAIN-SIP AddressFactory object used by this User Agent to
	 * build SIP Addresses
	 * 
	 * @return SIP header factory
	 */
	public static AddressFactory getAddressFactory() {
		return addressFactory;
	}

	/**
	 * Provides the User Agent a reference to a MediaSession object that
	 * contains all media information and a factory for NetworkConnection, that
	 * actually handles media negotiation during call.
	 * <p>
	 * MediaSession reference must be set before User Agent creation as this
	 * reference can no be changed. A new MediaSession object has to be created
	 * if media capabilities change: bandwidth, resolution. available codecs,
	 * etc. This requires to set the new MediaSession object into UaFactory and
	 * the User Agent re-initialization
	 * 
	 * @param mediaSession
	 */
	public static void setMediaSession(MediaSession mediaSession) {
		UaFactory.mediaSession = mediaSession;
	}

	/**
	 * Returns MediaSession previously set or null if any has been set
	 * 
	 * @return
	 */
	public static MediaSession getMediaSession() {
		return mediaSession;
	}

}