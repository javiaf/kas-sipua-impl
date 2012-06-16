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
import java.util.Random;

import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.header.CallIdHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.sip.transaction.CRegister;
import com.kurento.commons.ua.Call;
import com.kurento.commons.ua.CallAttributes;
import com.kurento.commons.ua.CallListener;
import com.kurento.commons.ua.ConferenceManager;
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.EndPointListener;
import com.kurento.commons.ua.event.EndPointEvent;
import com.kurento.commons.ua.event.EndpointEventEnum;
import com.kurento.commons.ua.exception.ServerInternalErrorException;
import com.kurento.commons.ua.timer.KurentoUaTimer;
import com.kurento.commons.ua.timer.KurentoUaTimerTask;

public class SipEndPointImpl implements EndPoint {

	private final static Logger log = LoggerFactory
			.getLogger(SipEndPointImpl.class);

	private boolean receiveCall;
	private int expires = 3600;
	private boolean isRegister = false;

	private String userName;
	private String realm;
	private Address sipUriAddress;
	private Address contactAddress;

	private UaImpl ua;
	private EndPointListener listener;
	// private List<TimerTask> scheduledTasks = new ArrayList<TimerTask>();

	private CallIdHeader registrarCallId;
	private static Random rnd = new Random(System.currentTimeMillis());
	private long cSeqNumber = Math.abs(rnd.nextLong() % 100000000);
	private String password;

	private KurentoUaTimer timer;
	SipEndPointTimerTask sipEndPointTimerTask;

	// ////////////
	//
	// CONSTRUCTOR
	//
	// ////////////

	protected SipEndPointImpl(String userName, String realm, String password,
			int expires, UaImpl ua, EndPointListener handler,
			Boolean receiveCall) throws ServerInternalErrorException {
		this.ua = ua;
		this.listener = handler;
		this.timer = ua.getTimer();
		this.userName = userName;
		this.realm = realm;
		this.expires = expires;
		this.password = password;
		try {
			this.sipUriAddress = UaFactory.getAddressFactory().createAddress(
					"sip:" + this.userName + "@" + this.realm);
		} catch (ParseException e) {
			throw new ServerInternalErrorException("Bad SipUri: sip:"
					+ this.userName + "@" + this.realm, e);
		}
		this.receiveCall = receiveCall;
		this.sipEndPointTimerTask = new SipEndPointTimerTask(this);

		// Verifications
		if (receiveCall && timer == null)
			throw new ServerInternalErrorException(
					"UA must provide a timer in order to enable Endpoint registration. Initializa a timer within the UA");

	}

	// //////////
	//
	// Getters
	//
	// //////////

	public Address getAddress() {
		return sipUriAddress;
	}

	@Override
	public String getUri() {
		return sipUriAddress.toString();
	}

	public Address getContact() {
		return contactAddress;
	}

	// ///////////////////////
	//
	// SipEndPoint Interface
	//
	// ///////////////////////

	@Override
	public void terminate() throws ServerInternalErrorException {
		if (isRegister()) {
			log.info("terminating endpoint");
			register(0);
		}
	}

	// ///////////////////////////
	//
	// Sip Transaction interface
	//
	// ///////////////////////////

	protected void incomingCall(SipContext incomingCall) {
		// notifyEvent can not be used as the source must be of type SipCall
		EndPointEvent event = new EndPointEvent(EndPointEvent.INCOMING_CALL,
				incomingCall);
		if (listener != null) {
			listener.onEvent(event);
		}
	}

	private synchronized void setIsRegister(boolean isRegister) {
		this.isRegister = isRegister;
	}

	private synchronized boolean isRegister() {
		return this.isRegister;
	}

	public void notifyEvent(EndpointEventEnum eventType) {
		EndPointEvent event = new EndPointEvent(eventType, this);
		if (EndPointEvent.REGISTER_USER_SUCESSFUL.equals(eventType)
				&& expires != 0) {

			log.debug("Endpoint becomes registered: " + eventType
					+ ", expires=" + expires);
			setIsRegister(true);

			// Set new Register schedule
			log.debug("Expires = " + expires);
			long period = (long) (getExpires() * 1000 * 0.8);
			log.debug("Period = " + expires);
			timer.schedule(sipEndPointTimerTask, period, period);

		} else if (EndPointEvent.REGISTER_USER_SUCESSFUL.equals(eventType)
				&& expires == 0
				|| EndPointEvent.REGISTER_USER_FAIL.equals(eventType)
				|| EndPointEvent.REGISTER_USER_NOT_FOUND.equals(eventType)) {

			log.debug("Endpoint becomes de-registered: " + eventType
					+ ", expires=" + expires);
			setIsRegister(false);
		}
		if (listener != null) {
			listener.onEvent(event);
		}
	}

	// /////////////////

	public void setRegistrarCallId(CallIdHeader registrarCallId) {
		this.registrarCallId = registrarCallId;
	}

	public CallIdHeader getRegistrarCallId() {
		return registrarCallId;
	}

	public long getcSeqNumber() {
		return cSeqNumber++;
	}

	public UaImpl getUa() {
		return this.ua;
	}

	protected void register(int expires) throws ServerInternalErrorException {
		log.debug("Request to register Endpoint: " + getUri());
		
		if (expires != 0) {
			// Calculate current contact address
			Address contactAddress;
			try {
				contactAddress = UaFactory.getAddressFactory()
						.createAddress(
								"sip:" + userName + "@"
										+ ua.getPublicAddress() + ":"
										+ ua.getPublicPort());
			} catch (ParseException e) {
				throw new ServerInternalErrorException(
						"Unable to create contact address while registering",
						e);
			}
			log.debug("Calculate contact address: " + contactAddress);
			if (!contactAddress.equals(this.contactAddress)) {
				log.debug("Detected contact address change from : " + this.contactAddress + " to " + contactAddress);
				// Contact address changed
				if (isRegister) {
					log.debug("Endpoint was already registered with old contact. De-Register first");
					// if still registered unregister previous contact
					register(0);
				}
				// Set new contact address
				this.contactAddress = contactAddress;
			}
		}
		
		if (receiveCall) {
			this.expires = expires;

			// Register contact
			SipProvider sipProvider = getUa().getSipProvider();

			if (sipProvider == null)
				throw new ServerInternalErrorException(
						"SipProvider is null when trying to register Endpoint: "
								+ userName + "@" + realm);

			this.registrarCallId = sipProvider.getNewCallId();

			try {
				this.registrarCallId.setCallId(getUa().getInstanceId()
						.toString());

			} catch (ParseException e) {
				log.warn("Unable to set callid for REGISTER request. Continue anyway");
			}

			// Before registration remove previous timers
			timer.cancel(sipEndPointTimerTask);
			
			CRegister register;
			register = new CRegister(this);
			register.sendRequest(null);
		} else {
			log.debug("Do not send REGISTER request. No call reception configured");
		}
	}

	public int getExpires() {
		return expires;
	}

	public String getPassword() {
		return password;
	}

	public String getUserName() {
		return userName;
	}

	@Override
	public Call dial(String remoteParty, CallListener callController)
			throws ServerInternalErrorException {
		return dial(remoteParty, new CallAttributes(), callController);
	}

	@Override
	public Call dial(String remoteParty, CallAttributes callAttributes,
			CallListener callController) throws ServerInternalErrorException {

		if (remoteParty != null) {
			log.debug("Creating new SipContext");
			SipContext sipContext = new SipContext(this);
			if (callAttributes != null) {
				sipContext.setCallAttributes(callAttributes);
			}
			sipContext.addListener(callController);
			Address remotePartyAddress;
			try {
				remotePartyAddress = UaFactory.getAddressFactory()
						.createAddress(remoteParty);
			} catch (ParseException e) {
				throw new ServerInternalErrorException(e.toString());
			}
			sipContext.connect(remotePartyAddress);
			return sipContext;
		} else {
			throw new ServerInternalErrorException(
					"Request connection to NULL remote party");
		}

	}

	private class SipEndPointTimerTask extends KurentoUaTimerTask {

		private SipEndPointImpl ep;

		public SipEndPointTimerTask(SipEndPointImpl ep) {
			this.ep = ep;
		}

		@Override
		public void run() {
			log.debug("sipEndpointTimerTask register");
			try {
				ep.register(ep.getExpires());
			} catch (ServerInternalErrorException e) {
				log.error("Timer SipEndPointTimerTask exception: " + e);
			}
		}

	}

	// TODO: Change to setListener as only one listener is allowed
	@Override
	public void addListener(EndPointListener listener) {
		this.listener = listener;

	}

	@Override
	public void removeListener(EndPointListener listener) {
		if (listener == this.listener) {
			listener = null;
		}

	}

	@Override
	public ConferenceManager getConferenceManager() {
		return null;
	}

}
