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

import javax.sip.address.Address;
import javax.sip.header.CallIdHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.sip.transaction.CRegister;
import com.kurento.commons.ua.Call;
import com.kurento.commons.ua.CallListener;
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
			KurentoUaTimer timer) throws ParseException,
			ServerInternalErrorException {
		this.ua = ua;
		this.listener = handler;
		this.timer = timer;
		this.userName = userName;
		this.realm = realm;
		this.expires = expires;
		this.password = password;
		this.sipUriAddress = UaFactory.getAddressFactory().createAddress(
				"sip:" + this.userName + "@" + this.realm);
		sipEndPointTimerTask = new SipEndPointTimerTask();

		ua.registerEndpoint(this);

	}

	protected void reconfigureEndPoint() throws ParseException,
			ServerInternalErrorException {

		this.contactAddress = UaFactory.getAddressFactory().createAddress(
				"sip:" + userName + "@" + ua.getPublicAddress() + ":"
						+ ua.getPublicPort());
		// The register will do the UA when it has connectivity
		// register();
	}

	// //////////
	//
	// Getters
	//
	// //////////

	public Address getAddress() {
		return sipUriAddress;
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
			setExpiresAndRegister(0);
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
		if (EndPointEvent.REGISTER_USER_SUCESSFUL.equals(eventType)) {
			setIsRegister(true);
		} else if (EndPointEvent.REGISTER_USER_FAIL.equals(eventType)
				|| EndPointEvent.REGISTER_USER_NOT_FOUND.equals(eventType)) {
			if (timer != null)
				timer.cancel(sipEndPointTimerTask);

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

	public void setExpiresAndRegister(int expires) {

		// Cancel previous schedulers
		this.expires = expires;
		log.debug("Expires = " + expires);
		timer.cancel(sipEndPointTimerTask);
		if (this.expires != 0) {
			// Set new Register schedule
			long period = (long) (getExpires() * 1000 * 0.8);
			log.debug("Period = " + expires);

			timer.schedule(sipEndPointTimerTask, period, period);
		}
		else
			// Send register with expires=0
			register();

	}

	public void register() {
		// Create call ID to avoid IP addresses that can be mangled by routers
		this.registrarCallId = getUa().getSipProvider().getNewCallId();
		try {
			this.registrarCallId.setCallId(getUa().getInstanceId().toString());
		} catch (ParseException e1) {
			log.warn("Unable to set REGISTER call ID", e1);
		}

		try {
			CRegister register;
			register = new CRegister(this);
			register.sendRequest(null);
		} catch (ServerInternalErrorException e) {
			log.error("REGISTER error", e);
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

	// protected void register(int expires) throws ServerInternalErrorException
	// {
	// log.info("Send REGISTER request: " + sipUriAddress + " > " +
	// contactAddress);
	// setExpiresAndRegister(expires);
	// }

	// TimerTask deprecated
	// private class RegisterTask extends TimerTask {
	//
	// private SipEndPointImpl user;
	//
	// protected RegisterTask(SipEndPointImpl user) {
	// this.user = user;
	// }
	//
	// @Override
	// public void run() {
	// try {
	// register();
	// } catch (ServerInternalErrorException e) {
	// log.error("Unable to re-register user:" + user
	// + ". Deleting from list of users");
	// user.notifyEvent(EndPointEvent.REGISTER_USER_FAIL);
	// }
	// }
	// }

	@Override
	public Call dial(String remoteParty, CallListener callController)
			throws ServerInternalErrorException {

		if (remoteParty != null) {
			log.debug("Creating new SipContext");
			SipContext sipContext = new SipContext(this);
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

		@Override
		public void run() {
			log.debug("sipEndpointTimerTask register");
			register();
		}

	}

}
