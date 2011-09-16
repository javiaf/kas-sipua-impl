package com.kurento.commons.sip.agent;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.sip.address.Address;
import javax.sip.header.CallIdHeader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kurento.commons.sip.SipCall;
import com.kurento.commons.sip.SipCallListener;
import com.kurento.commons.sip.SipEndPoint;
import com.kurento.commons.sip.SipEndPointListener;
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
	private static Timer timer = new Timer();

	// ////////////
	//
	// CONSTRUCTOR
	//
	// ////////////

	protected SipEndPointImpl(String userName, String realm, String password,
			int expires, UaImpl ua, SipEndPointListener handler)
			throws ParseException, ServerInternalErrorException {

		this.userName = userName;
		this.realm = realm;

		this.expires = expires;

		this.ua = ua;
		this.listener = handler;

		this.sipUriAddress = UaFactory.getAddressFactory().createAddress(
				"sip:" + userName + "@" + realm);
		this.contactAddress = UaFactory.getAddressFactory().createAddress(
				"sip:" + userName + "@" + ua.getLocalAddress() + ":"
						+ ua.getLocalPort());
		this.password = password;

		register();
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
		expires = 0;
		register();
	}

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
		CRegister register;

		// Register new contact
		register = new CRegister(this);
		register.sendRequest(null);

		// Set up a register timer if > 0
		if (expires > 0) {
			RegisterTask registerTask = this.new RegisterTask(this);
			long period = (long) (expires * 1000 * 0.8);
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