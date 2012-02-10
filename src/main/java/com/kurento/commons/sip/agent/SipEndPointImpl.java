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
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.sip.address.Address;
import javax.sip.header.CallIdHeader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.kurento.commons.sip.SipCall;
import com.kurento.commons.sip.SipCallListener;
import com.kurento.commons.sip.SipEndPoint;
import com.kurento.commons.sip.SipEndPointListener;
import com.kurento.commons.sip.android.RegisterService;
import com.kurento.commons.sip.android.SecondeService;
import com.kurento.commons.sip.event.SipEndPointEvent;
import com.kurento.commons.sip.event.SipEventType;
import com.kurento.commons.sip.exception.ServerInternalErrorException;
import com.kurento.commons.sip.transaction.COptions;
import com.kurento.commons.sip.transaction.CRegister;

public class SipEndPointImpl implements SipEndPoint {

	private final static Log log = LogFactory.getLog(SipEndPointImpl.class);

	private int expires = 3600;

	private String userName;
	private String realm;
	private Address sipUriAddress;
	private Address contactAddress;

	private UaImpl ua;
	private SipEndPointListener listener;
	private List<TimerTask> scheduledTasks = new ArrayList<TimerTask>();

	private CallIdHeader registrarCallId;
	private static Random rnd = new Random(System.currentTimeMillis());
	private long cSeqNumber = Math.abs(rnd.nextLong() % 100000000);
	private String password;

	// Timer
	private Timer timer = new Timer();
	private Context androidContext;
	private AlarmManager alarmManager;
	PendingIntent pendingIntent;

	// ////////////
	//
	// CONSTRUCTOR
	//
	// ////////////

	protected SipEndPointImpl(String userName, String realm, String password,
			int expires, UaImpl ua, SipEndPointListener handler, Context context)
			throws ParseException, ServerInternalErrorException {
		this.userName = userName;
		this.realm = realm;

		this.expires = expires;

		this.ua = ua;
		this.listener = handler;

		this.sipUriAddress = UaFactory.getAddressFactory().createAddress(
				"sip:" + userName + "@" + realm);
		this.contactAddress = UaFactory.getAddressFactory().createAddress(
				"sip:" + userName + "@" + ua.getPublicAddress() + ":"
						+ ua.getPublicPort());
		this.password = password;
		this.androidContext = context;

		createRegisterManger();
		register();
	}

	private void createRegisterManger() {
		log.debug("Creating register manager");
		SecondeService service = new SecondeService(this);
		long period = (long) (expires * 1000);
		log.debug("Register period set as " + period);
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
		log.info("terminating endpoint");
		expires = 0;
		alarmManager.cancel(pendingIntent);
		CRegister register;

		register = new CRegister(this);
		register.sendRequest(null);

	}

	// register();

	// ///////////////////////////
	//
	// Sip Transaction interface
	//
	// ///////////////////////////

	protected void incomingCall(SipContext incomingCall) {
		// notifyEvent can not be used as the source must be of type SipCall
		SipEndPointEvent event = new SipEndPointEvent(
				SipEndPointEvent.INCOMING_CALL, incomingCall);
		if (listener != null) {
			listener.onEvent(event);
		}
	}

	public void notifyEvent(SipEventType eventType) {
		SipEndPointEvent event = new SipEndPointEvent(eventType, this);
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

	public void setExpires(int expires) {
		this.expires = expires;
		try {
			this.register();
		} catch (ServerInternalErrorException e) {
			log.error(e);
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

	private void register() throws ServerInternalErrorException {
		log.info("Send REGISTER request: " + sipUriAddress);
		log.debug("Time is :" + new Date());
		CRegister register;

		// Register new contact
		register = new CRegister(this);
		register.sendRequest(null);

		// Set up a register timer if > 0
		if (expires > 0) {
			RegisterTask registerTask = this.new RegisterTask(this);
			long period = (long) (expires * 1000 * 0.8);
			log.debug("Register period is = " + period);
			timer.schedule(registerTask, period);
			scheduledTasks.add(registerTask);
		} else {
			for (TimerTask task : scheduledTasks) {
				task.cancel();
			}
		}

	}

	private class RegisterTask extends TimerTask {

		private SipEndPointImpl user;

		protected RegisterTask(SipEndPointImpl user) {
			this.user = user;
		}

		@Override
		public void run() {
			try {
				register();
			} catch (ServerInternalErrorException e) {
				log.error("Unable to re-register user:" + user
						+ ". Deleting from list of users");
				user.notifyEvent(SipEndPointEvent.REGISTER_USER_FAIL);
			}
		}
	}

	@Override
	public SipCall dial(String remoteParty, SipCallListener callController)
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

	@Override
	public void options(String remoteParty, SipCallListener callController)
			throws ServerInternalErrorException {
		Address remotePartyAddress;
		try {
			remotePartyAddress = UaFactory.getAddressFactory().createAddress(
					remoteParty);
		} catch (ParseException e) {
			throw new ServerInternalErrorException(e.toString());
		}
		new COptions(this, remotePartyAddress);

	}

}
