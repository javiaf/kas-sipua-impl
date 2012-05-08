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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.TooManyListenersException;
import java.util.UUID;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.sip.exception.SipTransactionException;
import com.kurento.commons.sip.transaction.CTransaction;
import com.kurento.commons.sip.transaction.SAck;
import com.kurento.commons.sip.transaction.SBye;
import com.kurento.commons.sip.transaction.SCancel;
import com.kurento.commons.sip.transaction.SInvite;
import com.kurento.commons.sip.transaction.STransaction;
import com.kurento.commons.sip.util.NatKeepAlive;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.UaStun;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

import de.javawi.jstun.attribute.MessageAttributeException;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.test.DiscoveryInfo;
import de.javawi.jstun.test.DiscoveryTest;
import de.javawi.jstun.util.UtilityException;

public class UaImpl implements SipListener, UaStun {

	private static final Logger log = LoggerFactory.getLogger(UaImpl.class);
	private final int NUMBER_TRY = 20;

	// Sip Stack
	private SipProvider sipProvider;
	private SipStack sipStack;

	// private UserAgentHeader userAgent;

	// Configuration parameters
	private String localAddress = "";
	private int localPort = 0;
	private String publicAddress = "";
	private int publicPort = 0;

	private NatKeepAlive keepAlive;
	// private TypeStun typeStun;

	private SipConfig config;

	private DiscoveryInfo info;

	// User List
	private HashMap<String, SipEndPointImpl> endPoints = new HashMap<String, SipEndPointImpl>();

	// private int contWifi = 0;

	// Register control
	private UUID instanceId;
	private int regId;

	// /////////////////////////
	//
	// CONSTRUCTOR
	//
	// /////////////////////////

	protected UaImpl(SipConfig config) throws Exception {
		this.config = config;
		this.localPort = config.getLocalPort();

		// instance-id: RFC5626
		/*
		 * According to RFC5626 instance-id must stay the same on UA reboot or
		 * power cycle. This implementation assigns temporal UUID that stays the
		 * same during UA's life cycle
		 */
		this.instanceId = UUID.randomUUID();
		this.regId = 1;

	}

	// //////////

	public void reconfigure() {
		log.info("Reconfigure SIP UA network connection");

		InetAddress localAddressNew = getLocalInterface(config
				.getLocalAddress());
		if (localAddressNew != null
				&& !localAddressNew.getHostAddress().equals(localAddress)) {
			// Detected Network interface change
			log.debug("Found network interface change: "
					+ localAddressNew.getHostAddress() + " <== " + localAddress);

			localAddress = localAddressNew.getHostAddress();
			// Check if user has request STUN
			if (config.getStunServerAddress() != null
					&& !"".equals((config.getStunServerAddress()))) {
				// YES, Try for NUMBER_TRY
				log.debug("STUN activated");
				int trying = 0;
				while (trying < NUMBER_TRY) {
					// STUN error => give up
					// IOError = socket already in use => try again and find a
					// free socket
					try {
						try {
							runStunTest();
						} catch (MessageAttributeParsingException e) {
							log.error("STUN test failed", e);
						} catch (MessageHeaderParsingException e) {
							log.error("STUN test failed", e);
						} catch (UtilityException e) {
							log.error("STUN test failed", e);
						} catch (MessageAttributeException e) {
							log.error("STUN test failed", e);
						}
						break;
					} catch (IOException e) {
						log.info("Address already in use:" + localAddress + ":"
								+ localPort);
						localPort++;
					}
				}
			} else {
				log.debug("STUN NOT activated");
				info = new DiscoveryInfo(localAddressNew);
				info.setPublicIP(localAddressNew);
				info.setPublicPort(publicPort);
				info.setLocalPort(localPort);
			}

			// Verify if NAT must be supported
			if (isNatSupported()) {
				log.debug("Activate NAT support");
				InetAddress publicInet = info.getPublicIP();
				publicAddress = publicInet.getHostAddress();
				publicPort = info.getPublicPort();
				localAddress = info.getLocalIP().getHostAddress();
				localPort = info.getLocalPort();
			} else {
				log.debug("De-Activate NAT support");
				publicAddress = localAddress;
				publicPort = localPort;
			}

			configureSipStack();

			for (SipEndPointImpl endpoint : endPoints.values()) {
				try {
					// endpoint.terminate();
					endpoint.reconfigureEndPoint();
					endpoint.setExpiresAndRegister(endpoint.getExpires());
				} catch (ServerInternalErrorException e) {
					log.error("Error finishing endpoint " + endpoint);
					log.info("Error finishing endpoint");
				} catch (ParseException e) {
					log.error("Error Parse endpoint");
					e.printStackTrace();
				}
			}
		}
	}

	// private BroadcastReceiver mReceiver = new BroadcastReceiver() {
	//
	// @Override
	// public void onReceive(Context context, Intent intent) {
	// String action = intent.getAction();
	// log.debug("Received ACTION: " + action );
	//
	// if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
	// NetworkInfo ni = intent.getExtras()
	// .getParcelable("networkInfo");
	// if (ni.getState().equals(NetworkInfo.State.CONNECTED)) {
	// InetAddress localAddressNew = getAndroidLocalAddress();
	// if (localAddressNew != null
	// && !localAddressNew.getHostAddress().equals(localAddress)) {
	//
	// log.debug("Found network interface change: "
	// + localAddressNew.getHostAddress() + " <== " + localAddress);
	//
	// localAddress = localAddressNew.getHostAddress();
	// // Check if user has request STUN
	// if (config.getStunAddress() != null
	// && !"".equals((config.getStunAddress()))) {
	// //YES, Try for NUMBER_TRY
	// log.debug("STUN activated");
	// int trying = 0;
	// while (trying < NUMBER_TRY) {
	// // STUN error => give up
	// // IOError = socket already in use => try again and find a free socket
	// try{
	// try {
	// runStunTest();
	// } catch (MessageAttributeParsingException e) {
	// log.error("STUN test failed",e);
	// } catch (MessageHeaderParsingException e) {
	// log.error("STUN test failed",e);
	// } catch (UtilityException e) {
	// log.error("STUN test failed",e);
	// } catch (MessageAttributeException e) {
	// log.error("STUN test failed",e);
	// }
	// break;
	// } catch (IOException e) {
	// log.info("Address already in use:"+ localAddress +":" + localPort);
	// localPort++;
	// }
	// }
	// } else {
	// log.debug("STUN NOT activated");
	// info = new DiscoveryInfo(localAddressNew);
	// info.setPublicIP(localAddressNew);
	// info.setPublicPort(publicPort);
	// info.setLocalPort(localPort);
	// }
	//
	// // Verify if NAT must be supported
	// if (isNatSupported()) {
	// log.debug("Activate NAT support");
	// InetAddress publicInet = info.getPublicIP();
	// publicAddress = publicInet.getHostAddress();
	// publicPort = info.getPublicPort();
	// localAddress = info.getLocalIP().getHostAddress();
	// localPort = info.getLocalPort();
	// }
	// else {
	// log.debug("De-Activate NAT support");
	// publicAddress = localAddress;
	// publicPort = localPort;
	// }
	//
	// configureSipStack();
	//
	// for (SipEndPointImpl endpoint : endPoints.values()) {
	// try {
	// //endpoint.terminate();
	// endpoint.reconfigureEndPoint();
	// endpoint.setExpiresAndRegister(3600);
	// } catch (ServerInternalErrorException e) {
	// log.error("Error finishing endpoint "
	// + endpoint);
	// log.info("Error finishing endpoint");
	// } catch (ParseException e) {
	// log.error("Error Parse endpoint");
	// e.printStackTrace();
	// }
	// }
	// }
	// }
	// }
	// }
	// };

	private void configureSipStack() {
		try {
			terminateSipStack();

			log.info("starting JAIN-SIP stack initializacion ...");

			Properties jainProps = new Properties();

			String outboundProxy = config.getProxyAddress() + ":"
					+ config.getProxyPort() + "/" + config.getTransport();
			jainProps.setProperty("javax.sip.OUTBOUND_PROXY", outboundProxy);

			jainProps.setProperty("javax.sip.STACK_NAME",
					"siplib_" + System.currentTimeMillis());
			jainProps.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER",
					"true");

			// Drop the client connection after we are done with the
			// transaction.
			jainProps.setProperty(
					"gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "true");
			jainProps.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", "100");
			jainProps.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", "100");

			// Set to 0 (or NONE) in your production code for max speed.
			// You need 16 (or TRACE) for logging traces. 32 (or DEBUG) for
			// debug +
			// traces.
			// Your code will limp at 32 but it is best for debugging.
			// jainProps.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "16");

			log.info("Stack properties: " + jainProps);

			// Create SIP factory objects

			sipStack = UaFactory.getSipFactory().createSipStack(jainProps);
			log.info("Local Addres = " + localAddress + ":" + localPort
					+ "; Transport = " + config.getTransport());
			ListeningPoint listeningPoint = sipStack.createListeningPoint(
					localAddress, localPort, config.getTransport());
			listeningPoint.setSentBy(publicAddress + ":" + publicPort);
			sipProvider = sipStack.createSipProvider(listeningPoint);
			sipProvider.addSipListener(this);

			if (!publicAddress.equals(localAddress)
					&& config.isEnableKeepAlive()) {
				log.debug("Creating keepalive for hole punching");
				keepAlive = new NatKeepAlive(config, listeningPoint);
				keepAlive.start();
			}
		} catch (PeerUnavailableException e) {
			log.error(e.getLocalizedMessage());
		} catch (TransportNotSupportedException e) {
			log.error(e.getLocalizedMessage());
		} catch (InvalidArgumentException e) {
			log.error(e.getLocalizedMessage());
		} catch (ParseException e) {
			log.error(e.getLocalizedMessage());
		} catch (ObjectInUseException e) {
			log.error(e.getLocalizedMessage());
		} catch (TooManyListenersException e) {
			log.error(e.getLocalizedMessage());
		}
	}

	private InetAddress getLocalInterface(String pattern) {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					log.debug("Found interface: IFNAME="
							+ intf.getDisplayName() + "; ADDR="
							+ inetAddress.getHostAddress());
					if (inetAddress instanceof Inet4Address) {
						if (!"".equals(pattern)) {
							// Search for a given interface or IP address
							if (intf.getDisplayName().equals(pattern)
									|| inetAddress.getHostAddress().equals(
											pattern)
									|| new String(inetAddress.getAddress())
											.equals(pattern)) {
								return inetAddress;
							}
						} else if (!inetAddress.isLoopbackAddress()) {
							// Search for first active interface
							return inetAddress;
						}
					}
				}
			}
		} catch (SocketException e) {
			log.error("Error while attaching to the network interface", e);
		}
		return null;
	}

	private boolean isNatSupported() {
		if (info == null)
			return false;
		if (info.isFullCone() || info.isOpenAccess()
				|| info.isPortRestrictedCone() || info.isRestrictedCone()) {
			return true;
		} else {
			return false;
		}
	}

	public void terminate() {

		log.info("SIP stack terminating ...");
		log.info("Stopping registered endpoits.");
		if (keepAlive != null) {
			log.info("Stopping hole punching");
			keepAlive.stop();
		}
		for (EndPoint endpoint : endPoints.values()) {
			try {
				endpoint.terminate();
			} catch (ServerInternalErrorException e) {
				log.error("Error finishing endpoint " + endpoint);
			}
		}

		terminateSipStack();
	}

	private void terminateSipStack() {
		if (sipStack != null) {
			while (true) {
				try {
					log.info("Delete Sip listening point");
					sipStack.deleteListeningPoint(sipProvider
							.getListeningPoint(config.getTransport()));
					break;
				} catch (ObjectInUseException e) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						continue;
					}
				}
			}
			sipProvider.removeSipListener(this);

			while (true) {
				try {
					sipStack.deleteSipProvider(sipProvider);
					break;
				} catch (ObjectInUseException e) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						continue;
					}
				}
			}
			sipStack.stop();
			log.info("SIP stack terminated");
		}
	}

	// /////////////////////////
	//
	// SIP LISTENER
	//
	// /////////////////////////

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

		// Check the request is addressed to one of managed users
		// Check if the message is addressed to one of the registered users

		SipEndPointImpl localParty = null;

		ServerTransaction serverTransaction;
		STransaction sTrns;
		Dialog dialog;
		String reqMethod = requestEvent.getRequest().getMethod();

		// Get transaction or create a new one. When a dialog exists
		// transactions are created automatically. This check is mainly to
		// manage dialog creating invites
		try {
			if ((serverTransaction = requestEvent.getServerTransaction()) == null) {
				// Create transaction
				serverTransaction = sipProvider
						.getNewServerTransaction(requestEvent.getRequest());
			}
			// Get local party or give up
			localParty = getLocalEndPoint(serverTransaction);

			// Check if SipContext has to be created
			if ((dialog = serverTransaction.getDialog()) != null) {
				log.debug("Created IN dialog transaction:"
						+ serverTransaction.getBranchId());
				if ((dialog.getApplicationData()) == null) {
					log.debug("New SipContext created for transaction: "
							+ serverTransaction.getBranchId());
					dialog.setApplicationData(new SipContext(localParty, dialog));
				} else {
					log.debug("Transaccion already has a SipContext associated");
				}
			} else {
				log.debug("Created OUT dialog transaction: "
						+ serverTransaction.getBranchId());
			}

			// Get Request method to create a proper transaction record
			if ((sTrns = (STransaction) serverTransaction.getApplicationData()) == null) {

				if (reqMethod.equals(Request.ACK)) {
					log.info("Detected ACK request");
					sTrns = new SAck(serverTransaction, localParty);
				} else if (reqMethod.equals(Request.INVITE)) {
					log.info("Detected INVITE request");
					sTrns = new SInvite(serverTransaction, localParty);
				} else if (reqMethod.equals(Request.BYE)) {
					log.info("Detected BYE request");
					sTrns = new SBye(serverTransaction, localParty);
				} else if (reqMethod.equals(Request.CANCEL)) {
					log.info("Detected Cancel request");
					sTrns = new SCancel(serverTransaction, localParty);
				} else {
					log.error("Unsupported method on request: " + reqMethod);
					sendStateless(Response.NOT_IMPLEMENTED,
							requestEvent.getRequest());
				}
				// Insert application data into server transaction
				serverTransaction.setApplicationData(sTrns);
			}
		} catch (Exception e) {
			log.error("Unable to process server transaction", e);
		}

	}

	@Override
	public void processResponse(ResponseEvent responseEvent) {
		log.info("\n" + "<<<<<<<< SIP response received <<<<<<\n"
				+ responseEvent.getResponse().toString()
				+ "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

		// Get transaction record for this response and process response
		// SipProvider searches a proper client transaction to each response.
		// if any is found it gives without any transaction
		ClientTransaction clientTransaction = responseEvent
				.getClientTransaction();
		if (clientTransaction == null) {
			// SIP JAIN was unable to find a proper transaction for this
			// response. The UAC will discard silently the request as stated by
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
		try {
			cTrns.processResponse(responseEvent);
		} catch (ServerInternalErrorException e) {
			log.error("Internal server error while procesing a response", e);
		}

	}

	@Override
	public void processTimeout(TimeoutEvent timeoutEvent) {
		if (timeoutEvent.isServerTransaction()) {
			ServerTransaction serverTransaction = timeoutEvent
					.getServerTransaction();
			log.error("Timeout event found for Server Transaction with ID: "
					+ serverTransaction.getBranchId());
			STransaction sTransaction;
			if ((sTransaction = (STransaction) serverTransaction
					.getApplicationData()) != null) {
				sTransaction.processTimeOut(timeoutEvent);
			}
		} else {
			ClientTransaction clientTransaction = timeoutEvent
					.getClientTransaction();
			log.error("Timeout event found for Client Transaction with ID:\n"
					+ clientTransaction.getBranchId());
			CTransaction cTransaction = (CTransaction) clientTransaction
					.getApplicationData();
			cTransaction.processTimeOut(timeoutEvent);
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

	private SipEndPointImpl getLocalEndPoint(ServerTransaction serverTransaction)
			throws SipTransactionException {
		Request request = serverTransaction.getRequest();

		// In server transaction localparty is addressed in TO header
		Address address;
		if ((address = ((ToHeader) request.getHeader(ToHeader.NAME))
				.getAddress()) == null) {
			String msg = "Malformed SIP request. Unable to get To header address";
			log.warn(msg);
			throw new SipTransactionException(msg);
		}
		SipURI sipUri;
		if (!address.getURI().isSipURI()) {
			String msg = "Unsupported URI format:"
					+ address.getURI().toString();
			log.warn(msg);
			throw new SipTransactionException(msg);
		} else {
			sipUri = (SipURI) address.getURI();
		}

		SipEndPointImpl epImpl;
		if ((epImpl = endPoints.get("sip:" + sipUri.getUser() + "@"
				+ sipUri.getHost())) != null) {
			return epImpl;
		} else {
			String msg = "End point not registered with this UA:"
					+ sipUri.toString();
			log.warn(msg);
			throw new SipTransactionException(msg);
		}
	}

	// //////////////
	//
	// Factory INTERFACE
	//
	// //////////////

	public SipProvider getSipProvider() {
		return sipProvider;
	}

	public SipStack getSipStack() {
		return sipStack;
	}

	// /////////////////////////
	//
	// CONFIGURATION INTERFACE
	//
	// /////////////////////////

	// public UserAgentHeader getUserAgentHeader() {
	// return userAgent;
	// }

	public String getLocalAddress() {
		return localAddress;
	}

	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}

	public int getLocalPort() {
		return localPort;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	public String getTransport() {
		if (config == null)
			return "UDP";
		return config.getTransport();
	}

	public int getMaxForwards() {
		if (config == null)
			return 70;
		return config.getMaxForards();
	}

	public String getPublicAddress() {
		return publicAddress;
	}

	public void setPublicAddress(String publicAddress) {
		this.publicAddress = publicAddress;
	}

	public int getPublicPort() {
		return publicPort;
	}

	public void setPublicPort(int publicPort) {
		this.publicPort = publicPort;
	}

	// ///////////
	//
	// User manager interface
	//
	// ///////////

	private void sendStateless(int code, Request request) {
		try {
			sipProvider.sendResponse(UaFactory.getMessageFactory()
					.createResponse(code, request));
		} catch (Exception e) {
			log.error("UA: Unable to send stateless response code:" + code
					+ ". GIVE UP!!!", e);
		}
	}

	private void runStunTest() throws IOException,
			MessageAttributeParsingException, MessageHeaderParsingException,
			UtilityException, MessageAttributeException {
		info = null;

		log.debug("RunStunTest = " + localAddress + ":" + localPort);
		InetAddress addr = InetAddress.getByName(localAddress);
		DiscoveryTest test = new DiscoveryTest(addr, localPort,
				config.getStunServerAddress(), config.getStunServerPort());

		info = test.test();

		log.debug("Stun test passed: Public Ip : "
				+ info.getPublicIP().getHostAddress() + " Public port : "
				+ info.getPublicPort());
	}

	@Override
	public void registerEndpoint(EndPoint endpoint) {
		if (!(endpoint instanceof SipEndPointImpl))
			return;
		endPoints.put(((SipEndPointImpl) endpoint).getAddress().toString(),
				(SipEndPointImpl) endpoint);
	}

	@Override
	public void unregisterEndpoint(EndPoint endpoint) {
		if (!(endpoint instanceof SipEndPointImpl))
			return;
		endPoints.remove(((SipEndPointImpl) endpoint).getAddress().toString());
	}

	@Override
	public DiscoveryInfo getConnectionType() throws Exception {
		if (info == null)
			throw new Exception("info is null");
		return info;
	}

	public UUID getInstanceId() {
		return instanceId;
	}

	public int getRegId() {
		return regId;
	}
}
