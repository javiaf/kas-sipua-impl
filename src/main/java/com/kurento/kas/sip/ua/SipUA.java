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

package com.kurento.kas.sip.ua;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.MessageFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;

import com.kurento.kas.sip.transaction.CRegister;
import com.kurento.kas.sip.transaction.CTransaction;
import com.kurento.kas.sip.util.AlarmUaTimer;
import com.kurento.kas.sip.util.NetworkUtilities;
import com.kurento.kas.ua.KurentoException;
import com.kurento.kas.ua.Register;
import com.kurento.kas.ua.RegisterHandler;
import com.kurento.kas.ua.UA;

public class SipUA extends UA {

	private static final Logger log = LoggerFactory.getLogger(SipUA.class
			.getSimpleName());

	private Context context;

	private static final String USER_AGENT = "KurentoAndroidUa/1.0.0";
	private UserAgentHeader userAgentHeader;

	// SIP factories
	private SipFactory sipFactory;
	private AddressFactory addressFactory;
	private HeaderFactory headerFactory;
	private MessageFactory messageFactory;

	// Sip Stack
	private SipProvider sipProvider;
	private SipStack sipStack;
	private SipListenerImpl sipListenerImpl = new SipListenerImpl();

	private AlarmUaTimer timer;
	private InetAddress localAddress;

	// Handlers
	private RegisterHandler registerHandler;

	private Map<String, SipRegister> localUris = new ConcurrentHashMap<String, SipRegister>();

	SipPreferences sipPreferences;

	public SipUA(Context context) throws KurentoSipException {
		super(context);

		try {
			this.context = context;
			sipPreferences = new SipPreferences(context);
			sipFactory = SipFactory.getInstance();
			addressFactory = sipFactory.createAddressFactory();
			headerFactory = sipFactory.createHeaderFactory();
			messageFactory = sipFactory.createMessageFactory();

			userAgentHeader = headerFactory
					.createUserAgentHeader(new ArrayList<String>() {
						private static final long serialVersionUID = 1L;
						{
							add(USER_AGENT);
						}
					});

			this.timer = new AlarmUaTimer(context);
			configureSipStack();
		} catch (Throwable t) {
			log.error("SipUA initialization error", t);
			throw new KurentoSipException("SipUA initialization error", t);
		}
	}

	@Override
	public void terminate() {
		terminateSipStack();
	}

	// ////////////////
	//
	// GETTERS & SETTERS
	//
	// ////////////////

	public SipPreferences getSipPreferences() {
		return sipPreferences;
	}

	public String getLocalAddress() {
		// TODO Return local address depending on STUN config
		return localAddress.getHostAddress();
	}

	public int getLocalPort() {
		// TODO Return local port depending on STUN config
		return sipPreferences.getLocalPort();
	}

	public AlarmUaTimer getTimer() {
		return timer;
	}

	public AddressFactory getAddressFactory() {
		return addressFactory;
	}

	public HeaderFactory getHeaderFactory() {
		return headerFactory;
	}

	public MessageFactory getMessageFactory() {
		return messageFactory;
	}

	public UserAgentHeader getUserAgentHeader() {
		return userAgentHeader;
	}

	public SipProvider getSipProvider() {
		return sipProvider;
	}

	public Address getContactAddress(String localUri) {
		SipRegister sipReg = localUris.get(localUri);
		if (sipReg != null)
			return sipReg.getAddress();
		return null;
	}

	// ////////////////
	//
	// HANDLERS
	//
	// ////////////////

	@Override
	public void setRegisterHandler(RegisterHandler registerHandler) {
		this.registerHandler = registerHandler;
	}

	public RegisterHandler getRegisterHandler() {
		return registerHandler;
	}

	// ////////////////////////////
	//
	// SIP STACK & INITIALIZATION
	//
	// ////////////////////////////

	private void configureSipStack() throws KurentoSipException {
		try {
			terminateSipStack(); // Just in case

			localAddress = NetworkUtilities.getLocalInterface(null,
					sipPreferences.isOnlyIpv4());

			// TODO Find configuration that supports TLS / DTLS
			// TODO Find configuration that supports TCP with persistent
			// connection
			log.info("starting JAIN-SIP stack initializacion ...");

			Properties jainProps = new Properties();

			String outboundProxy = sipPreferences.getProxyServerAddress() + ":"
					+ sipPreferences.getProxyServerPort() + "/"
					+ sipPreferences.getTransport();
			jainProps.setProperty("javax.sip.OUTBOUND_PROXY", outboundProxy);

			jainProps.setProperty("javax.sip.STACK_NAME",
					"siplib_" + System.currentTimeMillis());
			jainProps.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER",
					"true");

			jainProps.setProperty(
					"gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "true"); // By
																			// default
			jainProps.setProperty(
					"gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS", "true"); // By
																			// default
			jainProps.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", "100");

			log.info("Stack properties: " + jainProps);

			// Create SIP STACK
			sipStack = sipFactory.createSipStack(jainProps);
			// TODO get socket from SipStackExt to perform STUN test
			// TODO Verify socket transport to see if it is compatible with STUN

			// sipStack.getLocalAddressForTcpDst(
			// InetAddress.getAllByName("193.147.51.13")[0], 5060, addr,
			// 8989);

			// Create a listening point per interface
			log.info("Create listening point at: " + localAddress + ":"
					+ sipPreferences.getLocalPort() + "/"
					+ sipPreferences.getTransport());
			ListeningPoint listeningPoint = sipStack.createListeningPoint(
					localAddress.getHostAddress(),
					sipPreferences.getLocalPort(),
					sipPreferences.getTransport());
			// listeningPoint.setSentBy(publicAddress + ":" + publicPort);

			// Create SIP PROVIDER and add listening points
			sipProvider = sipStack.createSipProvider(listeningPoint);

			// Add User Agent as listener for the SIP provider
			sipProvider.addSipListener(sipListenerImpl);

			// TODO Re-register all local contacts

		} catch (Throwable t) {
			terminateSipStack();
			throw new KurentoSipException("Unable to instantiate a SIP stack",
					t);
		}
	}

	private void terminateSipStack() {
		if (sipStack != null && sipProvider != null) {
			log.info("Delete SIP listening points");

			for (ListeningPoint lp : sipProvider.getListeningPoints()) {
				try {
					sipStack.deleteListeningPoint(lp);
				} catch (ObjectInUseException e) {
					log.warn("Unable to delete SIP listening point: "
							+ lp.getIPAddress() + ":" + lp.getPort());
				}
			}

			sipProvider.removeSipListener(sipListenerImpl);
			try {
				sipStack.deleteSipProvider(sipProvider);
			} catch (ObjectInUseException e) {
				log.warn("Unable to delete SIP provider");
			}
			sipStack.stop();
			sipProvider = null;
			sipStack = null;
			log.info("SIP stack terminated");
		}
	}

	// ////////////////
	//
	// URI & REGISTER MANAGEMENT
	//
	// ////////////////

	@Override
	public void register(Register register) {
		// TODO Implement URI register
		// TODO Create contact address on register "sip:userName@address:port"
		// TODO Implement STUN in order to get public transport address. This
		// is not accurate at all, but at least give the chance
		// TODO STUN enabled then use public, STUN disabled then use private.
		// Do not check NAT type.

		try {
			log.debug("Request to register: " + register.getUri() + " for "
					+ sipPreferences.getRegExpires() + " seconds.");

			SipRegister sipReg = localUris.get(register.getUri());
			if (sipReg == null) {
				log.debug("There is not a previous register for "
						+ register.getUri() + ". Create new register.");
				Address contactAddress = addressFactory.createAddress("sip:"
						+ register.getUser() + "@"
						+ localAddress.getHostAddress() + ":"
						+ sipPreferences.getLocalPort());
				sipReg = new SipRegister(this, register, contactAddress);
				log.debug("Add into localUris ", register.getUri());
				localUris.put(register.getUri(), sipReg);
			}

			// Before registration remove previous timers
			timer.cancel(sipReg.getSipRegisterTimerTask());

			CRegister creg = new CRegister(this, sipReg,
					sipPreferences.getRegExpires());
			creg.sendRequest();
		} catch (ParseException e) {
			log.error("Unable to create contact address", e);
			registerHandler.onConnectionFailure(register);
		} catch (KurentoSipException e) {
			log.error("Unable to register", e);
			registerHandler.onConnectionFailure(register);
		} catch (KurentoException e) {
			log.error("Unable to create CRegister", e);
			registerHandler.onConnectionFailure(register);
		}
	}

	@Override
	public void unregister(Register register) {
		try {
			log.debug("Request to unregister: " + register.getUri());

			SipRegister sipReg = localUris.get(register.getUri());
			if (sipReg == null) {
				log.warn("There is not a previous register for "
						+ register.getUri());
				registerHandler.onRegistrationSuccess(register);
				return;
			}

			timer.cancel(sipReg.getSipRegisterTimerTask());
			CRegister creg = new CRegister(this, sipReg, 0);
			creg.sendRequest();
			localUris.remove(register.getUri());
		} catch (KurentoSipException e) {
			log.error("Unable to register", e);
			registerHandler.onConnectionFailure(register);
		} catch (KurentoException e) {
			log.error("Unable to create CRegister", e);
			registerHandler.onConnectionFailure(register);
		}
	}

	private class SipListenerImpl implements SipListener {

		@Override
		public void processDialogTerminated(DialogTerminatedEvent arg0) {
			// Nothing to do here
			log.info("Dialog Terminated. Perform clean up operations");
		}

		@Override
		public void processIOException(IOExceptionEvent arg0) {
			// Nothing to do here
			log.info("IO Exception");
		}

		@Override
		public void processRequest(RequestEvent requestEvent) {
			log.info("SIP request received\n"
					+ "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n"
					+ requestEvent.getRequest().toString() + "\n"
					+ "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			// TODO: complete
		}

		@Override
		public void processResponse(ResponseEvent responseEvent) {
			log.info("\n" + "<<<<<<<< SIP response received <<<<<<\n"
					+ responseEvent.getResponse().toString()
					+ "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

			// Get transaction record for this response and process response
			// SipProvider searches a proper client transaction to each
			// response.
			// if any is found it gives without any transaction
			ClientTransaction clientTransaction = responseEvent
					.getClientTransaction();
			if (clientTransaction == null) {
				// SIP JAIN was unable to find a proper transaction for this
				// response. The UAC will discard silently the request as stated
				// by RFC3261 18.1.2
				log.error("Unable to find a proper transaction matching response");
				return;
			}

			// Get the transaction application record and process response.
			CTransaction cTrns = (CTransaction) clientTransaction
					.getApplicationData();
			if (cTrns == null) {
				log.error("Server Internal Error (500): Empty application data for response transaction");
			}
			cTrns.processResponse(responseEvent);
		}

		@Override
		public void processTimeout(TimeoutEvent timeoutEvent) {
			log.warn("Transaction timeout:" + timeoutEvent.toString());
			// TODO: complete
		}

		@Override
		public void processTransactionTerminated(
				TransactionTerminatedEvent trnsTerminatedEv) {
			if (trnsTerminatedEv.isServerTransaction()) {
				log.info("Server Transaction terminated with ID: "
						+ trnsTerminatedEv.getServerTransaction().getBranchId());
			} else {
				log.info("Client Transaction terminated with ID: "
						+ trnsTerminatedEv.getClientTransaction().getBranchId());
			}
		}
	}

}