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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;

import com.kurento.commons.sip.transaction.CRegister;
import com.kurento.commons.sip.transaction.CTransaction;
import com.kurento.commons.sip.transaction.SAck;
import com.kurento.commons.sip.transaction.SBye;
import com.kurento.commons.sip.transaction.SCancel;
import com.kurento.commons.sip.transaction.SInvite;
import com.kurento.commons.sip.transaction.STransaction;
import com.kurento.commons.sip.util.AlarmUaTimer;
import com.kurento.commons.sip.util.KurentoUaTimer;
import com.kurento.commons.sip.util.KurentoUaTimerTask;
import com.kurento.kas.ua.CallDialingHandler;
import com.kurento.kas.ua.CallEstablishedHandler;
import com.kurento.kas.ua.CallRingingHandler;
import com.kurento.kas.ua.CallTerminatedHandler;
import com.kurento.kas.ua.ErrorHandler;
import com.kurento.kas.ua.KurentoException;
import com.kurento.kas.ua.Register;
import com.kurento.kas.ua.RegisterHandler;
import com.kurento.kas.ua.UA;

/**
 * This class provides a SIP UA implementation of UA interface
 * 
 * @author fjlopez
 * 
 */
public class SipUA extends UA {

	private static final Logger log = LoggerFactory.getLogger(SipUA.class);

	private SipListenerImpl sipListenerImpl = new SipListenerImpl();

	// User agent name
	private static final String USER_AGENT = "KurentoAndroidUa/1.0.0";
	private UserAgentHeader userAgent;

	// SIP Factory objects
	private SipFactory sipFactory;
	private AddressFactory addressFactory;
	private HeaderFactory headerFactory;
	private MessageFactory messageFactory;

	// Sip Stack
	private SipProvider sipProvider;
	private SipStack sipStack; // SipStackImpl

	// Handlers
	private ErrorHandler errorHandler;
	private RegisterHandler registerHandler;
	private CallDialingHandler dialingHandler;
	private CallEstablishedHandler establishedHandler;
	private CallRingingHandler ringingHandler;
	private CallTerminatedHandler terminatedHandler;

	// Configuration parameters
	// Network coniguration
	private String localAddressPattern;
	private boolean onlyIPv4 = true;
	private int localPort = 6060;
	private String localAddress;

	private String stunServerAddress;
	private int stunServerPort;

	// SIP configuration
	public static final String TRANSPORT = "TCP";
	private static final String PROXY_SERVER_ADDRESS = "193.147.51.13"; // "10.0.0.107";
																		// "193.147.51.13";
	private static final int PROXY_SERVER_PORT = 5060;
	public static final int MAX_FORWARDS = 70;
	public static final int EXPIRES = 3600;

	// Other config
	private boolean testMode = false;

	private Address contactAddress = null;
	private boolean isRegistered = false;
	private KurentoUaTimer timer;
	private RegisterTimerTask registerTimerTask;

	// List of managed URIs
	private Map<String, SipRegister> localUris = new ConcurrentHashMap<String, SipRegister>();

	// /////////////////////////
	//
	// CONSTRUCTOR
	//
	// /////////////////////////

	public SipUA(Context context) throws KurentoSipException {
		super(context);

		instantiateDefaultHandlers();
		// Create SIP stack infrastructure
		sipFactory = SipFactory.getInstance();

		// Create SIP factories
		try {
			addressFactory = sipFactory.createAddressFactory();
		} catch (PeerUnavailableException e) {
			log.error("Address Factory initialization error", e);
		}

		try {
			headerFactory = sipFactory.createHeaderFactory();
		} catch (PeerUnavailableException e) {
			log.error("Header Factory initialization error", e);
		}

		try {
			messageFactory = sipFactory.createMessageFactory();
		} catch (PeerUnavailableException e) {
			log.error("Message Factory initialization error", e);
		}

		// Name this User Agent
		try {
			userAgent = headerFactory
					.createUserAgentHeader(new ArrayList<String>() {
						private static final long serialVersionUID = 1L;
						{
							add(USER_AGENT);
						}
					});
		} catch (ParseException e) {
			log.error("User Agent header initialization error", e);
		}

		this.timer = new AlarmUaTimer(context); // TODO: complete timer impl
		configureSipStack();
	}

	@Override
	public void terminate() {
		terminateSipStack();
	}

	// //////////
	//
	// NETWORK LISTENER INTERFACE
	//
	// //////////

	// TODO Detect network interface changes and reconfigure the SIP stack

	// ////////////////
	//
	// GETTERS & SETTERS
	//
	// ////////////////

	public String getLocalAddress() {
		// TODO Return local address depending on STUN config
		return localAddress;
	}

	public int getLocalPort() {
		// TODO Return local port depending on STUN config
		return localPort;
	}

	private synchronized void setIsRegistered(boolean isRegistered) {
		this.isRegistered = isRegistered;
	}

	private synchronized boolean isRegistered() {
		return this.isRegistered;
	}

	// ////////////////
	//
	// FACTORY GETTERS
	//
	// ////////////////

	/**
	 * Returns the User Agent header as defined by JAIN-SIP
	 * 
	 * @return User Agent Header
	 */
	public UserAgentHeader getUserAgentHeader() {
		return userAgent;
	}

	/**
	 * Returns the JAIN-SIP SipFactory object that holds the SIP stack this User
	 * Agent utilizes for message interchange
	 * 
	 * @return SIP factory
	 */
	public SipFactory getSipFactory() {
		return sipFactory;
	}

	/**
	 * Returns the JAIN-SIP MessageFactory object used by this User Agent to
	 * build SIP messages
	 * 
	 * @return SIP message factory
	 */
	public MessageFactory getMessageFactory() {
		return messageFactory;
	}

	/**
	 * Returns the JAIN-SIP HeaderFactory object used by this User Agent to
	 * build SIP headers
	 * 
	 * @return SIP header factory
	 */
	public HeaderFactory getHeaderFactory() {
		return headerFactory;
	}

	/**
	 * Returns the JAIN-SIP AddressFactory object used by this User Agent to
	 * build SIP Addresses
	 * 
	 * @return SIP header factory
	 */
	public AddressFactory getAddressFactory() {
		return addressFactory;
	}

	public SipProvider getSipProvider() {
		return sipProvider;
	}

	public SipStack getSipStack() {
		return sipStack;
	}

	// ////////////////////////////
	//
	// SIP STACK & INITIALIZATION
	//
	// ////////////////////////////

	private void configureSipStack() throws KurentoSipException {
		try {
			terminateSipStack(); // Just in case

			InetAddress addr;
			try {
				addr = getLocalInterface(localAddressPattern, onlyIPv4);
				this.localAddress = addr.getHostAddress();
			} catch (IOException e) {
				log.error("Unable to get local interface.", e);
				throw new KurentoSipException("Unable to get local interface.",
						e);
			}

			// TODO Find configuration that supports TLS / DTLS
			// TODO Find configuration that supports TCP with persistent
			// connection
			log.info("starting JAIN-SIP stack initializacion ...");

			Properties jainProps = new Properties();

			String outboundProxy = PROXY_SERVER_ADDRESS + ":"
					+ PROXY_SERVER_PORT + "/" + TRANSPORT;
			jainProps.setProperty("javax.sip.OUTBOUND_PROXY", outboundProxy);

			jainProps.setProperty("javax.sip.STACK_NAME",
					"siplib_" + System.currentTimeMillis());
			jainProps.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER",
					"true");

			jainProps.setProperty(
					"gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "true");
			jainProps.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", "100");

			// jainProps.setProperty("gov.nist.javax.sip.TLS_SECURITY_POLICY",
			// this.getClass().getName());

			// Set to 0 (or NONE) in your production code for max speed.
			// You need 16 (or TRACE) for logging traces. 32 (or DEBUG) for
			// debug + traces.
			// Your code will limp at 32 but it is best for debugging.
			// jainProps.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "16");

			if (testMode) {
				// jainProps.setProperty("gov.nist.javax.sip.EARLY_DIALOG_TIMEOUT_SECONDS",
				// "10");
				jainProps.setProperty(
						"gov.nist.javax.sip.MAX_TX_LIFETIME_INVITE", "10");
			}

			log.info("Stack properties: " + jainProps);

			// Create SIP STACK
			sipStack = sipFactory.createSipStack(jainProps); // new
			// SipStackImpl(jainProps);
			// TODO get socket from SipStackExt to perform STUN test
			// TODO Verify socket transport to see if it is compatible with STUN

			// sipStack.getLocalAddressForTlsDst(arg0, arg1, arg2)

			// Create a listening point per interface
			log.info("Create listening point at: " + localAddress + ":"
					+ localPort + "/" + TRANSPORT);
			ListeningPoint listeningPoint = sipStack.createListeningPoint(
					localAddress, localPort, TRANSPORT);
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
			log.debug("Request to register: " + register.getUri());

			// TODO: only once
			Address contactAddress = addressFactory
					.createAddress("sip:" + register.getUser() + "@"
							+ localAddress + ":" + localPort);
			SipRegister sipRegister = new SipRegister(register, contactAddress);
			log.debug("Add into localUris ", register.getUri());
			localUris.put(register.getUri(), sipRegister);

			// Before registration remove previous timers
			// FIXME:
			timer.cancel(registerTimerTask);

			CRegister creg = new CRegister(this, sipRegister, EXPIRES);
			creg.sendRequest();
			registerTimerTask = new RegisterTimerTask(register, registerHandler);
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
	public void setRegisterHandler(RegisterHandler registerHandler) {
		this.registerHandler = registerHandler;
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

			SipRegister sipRegister = new SipRegister(register, contactAddress);
			CRegister creg = new CRegister(this, sipRegister, 0);
			creg.sendRequest();
			// FIXME: finish timer
			timer.cancel(registerTimerTask);
			localUris.remove(register.getUri());
		} catch (KurentoSipException e) {
			log.error("Unable to register", e);
			registerHandler.onConnectionFailure(register);
		} catch (KurentoException e) {
			log.error("Unable to create CRegister", e);
			registerHandler.onConnectionFailure(register);
		}
	}

	// TODO Implement re-register timer (expires time enabled by configuration)

	// TODO Implement keep-alive timer (enabled by configuration)

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

	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

	public RegisterHandler getRegisterHandler() {
		return registerHandler;
	}

	public CallDialingHandler getCallDialingHandler() {
		return dialingHandler;
	}

	public CallEstablishedHandler getCallEstablishedHandler() {
		return establishedHandler;
	}

	public CallRingingHandler getCallRingingHandler() {
		return ringingHandler;
	}

	public CallTerminatedHandler getCallTerminatedHandler() {
		return terminatedHandler;
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

			ServerTransaction serverTransaction;
			try {
				if ((serverTransaction = requestEvent.getServerTransaction()) == null) {
					// Create transaction
					serverTransaction = sipProvider
							.getNewServerTransaction(requestEvent.getRequest());
				}
			} catch (TransactionAlreadyExistsException e) {
				log.warn("Request already has an active transaction. It shouldn't be delivered by SipStack to the SIPU-UA");
				return;
			} catch (TransactionUnavailableException e) {
				log.warn("Unable to get ServerTransaction for request");
				return;
			}

			try {

				// Check if this transaction addressed to this UA
				String requestUri = requestEvent.getRequest().getRequestURI()
						.toString();
				if (!localUris.containsKey(requestUri)) {
					// Request is addressed to unknown URI
					log.info("SIP transaction for unknown URI: " + requestUri);
					Response response = messageFactory.createResponse(
							Response.NOT_FOUND, serverTransaction.getRequest());
					serverTransaction.sendResponse(response);
					return;
				}

				// Check if the SIPCALL has to be created
				Dialog dialog;
				if ((dialog = serverTransaction.getDialog()) != null
						&& dialog.getApplicationData() == null) {
					log.debug("Create SipCall for transaction: "
							+ serverTransaction.getBranchId());
					SipCall call = new SipCall(SipUA.this, dialog);
					dialog.setApplicationData(call);
				} else {
					log.debug("Transaccion already has an associated SipCall");
				}

				// Get Request method to create a proper transaction record
				STransaction sTrns;
				if ((sTrns = (STransaction) serverTransaction
						.getApplicationData()) == null) {
					String reqMethod = requestEvent.getRequest().getMethod();
					if (reqMethod.equals(Request.ACK)) {
						log.info("Detected ACK request");
						sTrns = new SAck(SipUA.this, serverTransaction);
					} else if (reqMethod.equals(Request.INVITE)) {
						log.info("Detected INVITE request");
						sTrns = new SInvite(SipUA.this, serverTransaction);
					} else if (reqMethod.equals(Request.BYE)) {
						log.info("Detected BYE request");
						sTrns = new SBye(SipUA.this, serverTransaction);
					} else if (reqMethod.equals(Request.CANCEL)) {
						log.info("Detected CANCEL request");
						sTrns = new SCancel(SipUA.this, serverTransaction);
					} else {
						log.error("Unsupported method on request: " + reqMethod);
						Response response = messageFactory.createResponse(
								Response.NOT_IMPLEMENTED,
								requestEvent.getRequest());
						serverTransaction.sendResponse(response);
					}
					// Insert application data into server transaction
					serverTransaction.setApplicationData(sTrns);
				}

			} catch (Exception e) {
				log.warn("Unable to process server transaction", e);
			}

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
				// by
				// RFC3261 18.1.2
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
			try {
				if (timeoutEvent.getClientTransaction() != null) {
					CTransaction cTrns = (CTransaction) timeoutEvent
							.getClientTransaction().getApplicationData();
					if (cTrns != null)
						cTrns.processTimeout();
					timeoutEvent.getClientTransaction().terminate();

				} else if (timeoutEvent.getServerTransaction() != null) {
					STransaction sTrns = (STransaction) timeoutEvent
							.getClientTransaction().getApplicationData();
					if (sTrns != null)
						sTrns.processTimeout();
					timeoutEvent.getServerTransaction().terminate();
				}

			} catch (ObjectInUseException e) {
				log.error("Unable to handle timeouts");
			}
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

	private class RegisterTimerTask extends KurentoUaTimerTask {

		private Register register;
		private RegisterHandler registerHandler;

		public RegisterTimerTask(Register register,
				RegisterHandler registerHandler) {
			this.register = register;
			this.registerHandler = registerHandler;
		}

		@Override
		public void run() {
			log.debug("sipEndpointTimerTask register");
			register(register);
		}

	}

	/*
	 * Get the first reachable address matching parameter pattern
	 */
	private InetAddress getLocalInterface(String pattern, boolean onlyIPv4)
			throws IOException {

		Enumeration<NetworkInterface> intfEnum = NetworkInterface
				.getNetworkInterfaces();

		while (intfEnum.hasMoreElements()) {
			NetworkInterface intf = intfEnum.nextElement();
			Enumeration<InetAddress> addrEnum = intf.getInetAddresses();
			while (addrEnum.hasMoreElements()) {
				InetAddress inetAddress = addrEnum.nextElement();
				log.debug("Found interface: IFNAME=" + intf.getDisplayName()
						+ "; ADDR=" + inetAddress.getHostAddress());

				// Check if only IPV4 is requested
				if (onlyIPv4 && !(inetAddress instanceof Inet4Address)) {
					continue;
				}

				// If address matches pattern return it. No matter what kind of
				// address it is
				if (pattern != null && !"".equals(pattern)) {

					if (intf.getDisplayName().equals(pattern)
							|| inetAddress.getHostAddress().equals(pattern)
							|| inetAddress.getHostAddress().equals(pattern)) {
						return inetAddress;
					}
				} else {
					// By default do not return multicast addresses (224...)
					if (inetAddress.isMulticastAddress()) {
						continue;
					}
					// By default do not return loopback addresses (127...)
					if (inetAddress.isLoopbackAddress()) {
						continue;
					}
					// By default do not return link local address (169...)
					if (inetAddress.isLinkLocalAddress()) {
						continue;
					}
					if (inetAddress.isReachable(3000)) {
						// Return only reachable interfaces
						return inetAddress;
					}
				}
			}
		}
		return null;
	}

	private void instantiateDefaultHandlers() {
		registerHandler = new RegisterHandler() {

			@Override
			public void onRegistrationSuccess(Register register) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onConnectionFailure(Register register) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAuthenticationFailure(Register register) {
				// TODO Auto-generated method stub

			}
		};
	}

}