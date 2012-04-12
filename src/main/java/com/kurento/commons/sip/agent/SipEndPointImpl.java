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
//import java.util.ArrayList;
import java.util.Date;
//import java.util.List;
import java.util.Random;
//import java.util.Timer;
//import java.util.TimerTask;

import javax.sip.address.Address;
import javax.sip.header.CallIdHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.kurento.commons.sip.android.RegisterService;
//import com.kurento.commons.sip.android.SecondeService;
import com.kurento.commons.sip.transaction.CRegister;
import com.kurento.commons.ua.Call;
import com.kurento.commons.ua.CallListener;
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.EndPointListener;
import com.kurento.commons.ua.event.EndPointEvent;
import com.kurento.commons.ua.event.EndpointEventEnum;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public class SipEndPointImpl implements EndPoint {

	private final static Logger log = LoggerFactory
			.getLogger(SipEndPointImpl.class);

	private int expires = 3600;
	private boolean isRegister = false;

	private String userName;
	private String realm;
	private Address sipUriAddress;
	private Address contactAddress;

	private UaImpl ua;
	private EndPointListener listener;
	//private List<TimerTask> scheduledTasks = new ArrayList<TimerTask>();

	private CallIdHeader registrarCallId;
	private static Random rnd = new Random(System.currentTimeMillis());
	private long cSeqNumber = Math.abs(rnd.nextLong() % 100000000);
	private String password;

	// Timer deprecated by android alarm manager
	//private Timer timer = new Timer();
	private Context androidContext;
	private AlarmManager alarmManager;
	PendingIntent pendingIntent;

	// ////////////
	//
	// CONSTRUCTOR
	//
	// ////////////

	protected SipEndPointImpl(String userName, String realm, String password,
			int expires, UaImpl ua, EndPointListener handler, Context context)
			throws ParseException, ServerInternalErrorException {
		this.ua = ua;
		this.listener = handler;
		this.androidContext = context;
		this.userName = userName;
		this.realm = realm;
		this.expires = expires;
		this.password = password;
		this.sipUriAddress = UaFactory.getAddressFactory().createAddress(
				"sip:" + this.userName + "@" + this.realm);

		reconfigureEndPoint();
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

	private void createRegisterManger() {
		log.info("Creating register manager");
		//SecondeService service = new SecondeService(this);
		long period = (long) (getExpires() * 1000);
		if (period == 0) {
			log.info("Expires is 0.");
			return;
		}
		log.info("Register period set as " + period);
		alarmManager = (AlarmManager) androidContext
				.getSystemService(Context.ALARM_SERVICE);
		Intent myIntent = new Intent(androidContext, RegisterService.class);
		log.info("Intend is " + myIntent.toString());
		pendingIntent = PendingIntent
				.getService(androidContext, 0, myIntent, 0);
		log.info("pendingIntent is " + pendingIntent.toString()
				+ "period is : " + period);
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 0, period,
				pendingIntent);
		log.info("SipEndpoint initialized.");
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
			if (alarmManager != null)
				alarmManager.cancel(pendingIntent);
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
		return ua;
	}

	public void setExpiresAndRegister(int expires) {
		if (expires == 0) {
			if (alarmManager != null)
				alarmManager.cancel(pendingIntent);
		} else if (this.expires != expires) {
			if (alarmManager != null)
				alarmManager.cancel(pendingIntent);
			createRegisterManger();
		}
		this.expires = expires;
		try {
			CRegister register;
			register = new CRegister(this);
			register.sendRequest(null);
		} catch (ServerInternalErrorException e) {
			log.error(e.toString());
			e.printStackTrace();
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

	protected void register() throws ServerInternalErrorException {
		log.info("Send REGISTER request: " + sipUriAddress + " > " + contactAddress);
		log.debug("Time is :" + new Date());
		expires = 3600;

		setExpiresAndRegister(expires);
	}

	// TimerTask deprecated
//	private class RegisterTask extends TimerTask {
//
//		private SipEndPointImpl user;
//
//		protected RegisterTask(SipEndPointImpl user) {
//			this.user = user;
//		}
//
//		@Override
//		public void run() {
//			try {
//				register();
//			} catch (ServerInternalErrorException e) {
//				log.error("Unable to re-register user:" + user
//						+ ". Deleting from list of users");
//				user.notifyEvent(EndPointEvent.REGISTER_USER_FAIL);
//			}
//		}
//	}

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

}
