package com.tikal.sip.agent;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Properties;

import javaxt.sip.ClientTransaction;
import javaxt.sip.Dialog;
import javaxt.sip.DialogTerminatedEvent;
import javaxt.sip.IOExceptionEvent;
import javaxt.sip.ListeningPoint;
import javaxt.sip.ObjectInUseException;
import javaxt.sip.RequestEvent;
import javaxt.sip.ResponseEvent;
import javaxt.sip.ServerTransaction;
import javaxt.sip.SipListener;
import javaxt.sip.SipProvider;
import javaxt.sip.SipStack;
import javaxt.sip.TimeoutEvent;
import javaxt.sip.TransactionTerminatedEvent;
import javaxt.sip.address.Address;
import javaxt.sip.address.SipURI;
import javaxt.sip.header.ToHeader;
import javaxt.sip.header.UserAgentHeader;
import javaxt.sip.message.Request;
import javaxt.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tikal.sip.SipEndPoint;
import com.tikal.sip.SipEndPointListener;
import com.tikal.sip.UA;
import com.tikal.sip.exception.ServerInternalErrorException;
import com.tikal.sip.exception.SipTransactionException;
import com.tikal.sip.transaction.CTransaction;
import com.tikal.sip.transaction.SAck;
import com.tikal.sip.transaction.SBye;
import com.tikal.sip.transaction.SInvite;
import com.tikal.sip.transaction.STransaction;
import com.tikal.sip.util.SipConfig;

public class UaImpl implements SipListener, UA{
		
	private static final Log log = LogFactory.getLog(UaImpl.class);
		
	// Sip Stack
	private SipProvider sipProvider;
	private SipStack sipStack;
	
	private UserAgentHeader userAgent;
	
	// Configuration parameters
	private String localAddress = "127.0.0.1";
	private int localPort = 5060;
	private String proxyAddress = "127.0.0.1";
	private int proxyPort = 5060;
	private String transport = "UDP";
	private int maxForwards = 70;
		
	// User List
	private HashMap<String,SipEndPointImpl> endPoints = new HashMap<String,SipEndPointImpl>();

	///////////////////////////
	//
	// CONSTRUCTOR
	//
	///////////////////////////
	
	protected UaImpl (SipConfig config) throws Exception {

		this.localAddress = config.getLocalAddress();
		this.localPort = config.getLocalPort();
		this.proxyAddress = config.getProxyAddress();
		this.proxyPort = config.getProxyPort();
		this.transport = config.getTransport();
		this.maxForwards = config.getMaxForards();

		log.info("starting JAIN-SIP stack initializacion ...");

		Properties jainProps = new Properties();

		String outboundProxy = proxyAddress + ":" + proxyPort + "/" + transport;
		jainProps.setProperty("javax.sip.OUTBOUND_PROXY", outboundProxy);

		jainProps.setProperty("javax.sip.STACK_NAME", "siplib_" + System.currentTimeMillis());
		jainProps.setProperty("com.tikal.javax.sip.REENTRANT_LISTENER", "true");

		// Drop the client connection after we are done with the transaction.
		jainProps.setProperty("com.tikal.javax.sip.CACHE_CLIENT_CONNECTIONS", "true");
		jainProps.setProperty("com.tikal.javax.sip.THREAD_POOL_SIZE", "100");
		// Set to 0 (or NONE) in your production code for max speed.
		// You need 16 (or TRACE) for logging traces. 32 (or DEBUG) for debug +
		// traces.
		// Your code will limp at 32 but it is best for debugging.
//		jainProps.setProperty("com.tikal.javax.sip.TRACE_LEVEL", "16");

		log.info("Stack properties: " + jainProps);

		// Create SIP factory objects
		sipStack = UaFactory.getSipFactory().createSipStack(jainProps);


		ListeningPoint listeningPoint = sipStack.createListeningPoint(localAddress,localPort, transport);
		sipProvider = sipStack.createSipProvider(listeningPoint);
		
		
		sipProvider.addSipListener(this);
		log.info("SIP stack initializacion complete. Listening on " + localAddress + ":" + localPort +"/" + transport);
				
	}

	public void terminate() {
		log.info("SIP stack terminating ...");

		while (true) {
			try {
				log.info("Delete Sip listening point");
				sipStack.deleteListeningPoint(sipProvider.getListeningPoint(transport));
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
		sipProvider = null;
		log.info("SIP stack terminated");
	}

	///////////////////////////
	//
	// SIP LISTENER
	//
	///////////////////////////	
	
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
				 + requestEvent.getRequest().toString() +"\n"
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
				serverTransaction = sipProvider.getNewServerTransaction(requestEvent.getRequest());
			}
			// Get local party or give up
			localParty = getLocalEndPoint(serverTransaction);
			
			// Check if SipContext has to be created
			if ( (dialog = serverTransaction.getDialog()) != null) {
				log.debug("Created IN dialog transaction:" + serverTransaction.getBranchId());
				if ( ( dialog.getApplicationData()) == null) {
					log.debug("New SipContext created for transaction: " + serverTransaction.getBranchId());
					dialog.setApplicationData( new SipContext(localParty,dialog));
				} else {
					log.debug("Transaccion already has a SipContext associated");
				}
			} else {
				log.debug("Created OUT dialog transaction: " + serverTransaction.getBranchId());
			}
		
			// Get Request method to create a proper transaction record
			if ((sTrns = (STransaction) serverTransaction.getApplicationData()) == null) {
					
				if (reqMethod.equals(Request.ACK)) {
					log.info("Detected ACK request");
					sTrns = new SAck(serverTransaction,localParty);
				} else if (reqMethod.equals(Request.INVITE)) {
					log.info("Detected INVITE request");
					sTrns = new SInvite(serverTransaction,localParty);
				} else if (reqMethod.equals(Request.BYE)) {
					log.info("Detected BYE request");
					sTrns = new SBye(serverTransaction, localParty);
				} else {
					log.error("Unsupported method on request: " + reqMethod);
					sendStateless(Response.NOT_IMPLEMENTED, requestEvent.getRequest());
				}
				// Insert application data into server transaction
				serverTransaction.setApplicationData(sTrns);
			}
		} catch (Exception e) {
			log.error("Unable to process server transaction",e);
		}

	}

	@Override
	public void processResponse(ResponseEvent responseEvent) {
		log.info("\n"
				 + "<<<<<<<< SIP response received <<<<<<\n"
				 + responseEvent.getResponse().toString()
		         + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

		// Get transaction record for this response and process response
		// SipProvider searches a proper client transaction to each response.
		// if any is found it gives without any transaction
		ClientTransaction clientTransaction = responseEvent.getClientTransaction();
		if (clientTransaction == null) {
			// SIP JAIN was unable to find a proper transaction for this
			// response. The UAC will discard silently the request as stated by
			// RFC3261 18.1.2
			log.error("Unable to find a proper transaction matching response");
			return;
		}

		// Get the transaction application record and process response.
		CTransaction cTrns = (CTransaction) clientTransaction.getApplicationData();
		if (cTrns == null) {
			log.error("Server Internal Error (500): Empty application data for response transaction");
		}
		try {
			cTrns.processResponse(responseEvent);
		} catch (ServerInternalErrorException e) {
			log.error("Internal server error while procesing a response",e);
		}

	}

	@Override
	public void processTimeout(TimeoutEvent timeoutEvent) {
		if (timeoutEvent.isServerTransaction()) {
			ServerTransaction serverTransaction = timeoutEvent.getServerTransaction();
			log.error("Timeout event found for Server Transaction with ID: " + serverTransaction.getBranchId());
			STransaction sTransaction;
			if ( (sTransaction = (STransaction) serverTransaction.getApplicationData()) != null ){
				sTransaction.processTimeOut(timeoutEvent);
			}
		} else {
			ClientTransaction clientTransaction = timeoutEvent.getClientTransaction();
			log.error("Timeout event found for Client Transaction with ID:\n" + clientTransaction.getBranchId());
			CTransaction cTransaction = (CTransaction) clientTransaction.getApplicationData();
			cTransaction.processTimeOut(timeoutEvent);
		}
	}

	@Override
	public void processTransactionTerminated(TransactionTerminatedEvent trnsTerminatedEv) {
		if (trnsTerminatedEv.isServerTransaction()) {
			log.info("Server Transaction terminated with ID: "
			        + trnsTerminatedEv.getServerTransaction().getBranchId());
		} else {
			log.info("Client Transaction terminated with ID: "
			        + trnsTerminatedEv.getClientTransaction().getBranchId());
		}		
	}	
	
	private SipEndPointImpl getLocalEndPoint(ServerTransaction serverTransaction) throws SipTransactionException{
		Request request = serverTransaction.getRequest();
		
		// In server transaction localparty is addressed in TO header
		Address address;
		if ( (address = ((ToHeader) request.getHeader(ToHeader.NAME)).getAddress()) == null) {
			String msg = "Malformed SIP request. Unable to get To header address";
			log.warn(msg);
			throw new SipTransactionException(msg);
		}
		SipURI sipUri;
		if ( ! address.getURI().isSipURI()) {
			String msg = "Unsupported URI format:" + address.getURI().toString();
			log.warn(msg);
			throw new SipTransactionException(msg);
		} else {
			sipUri = (SipURI) address.getURI();
		}
		
		SipEndPointImpl epImpl;
		if ((epImpl= endPoints.get(sipUri.getUser() +"@"+sipUri.getHost()) ) != null ) {
			return epImpl;
		} else {
			String msg = "End point not registered with this UA:" + sipUri.toString();
			log.warn(msg);
			throw new SipTransactionException(msg);
		}
	}
	
	////////////////
	//
	// Factory INTERFACE
	//
	////////////////
	
	public SipProvider getSipProvider() {
		return sipProvider;
	}
	
	public SipStack getSipStack(){
		return sipStack;
	}
	
	///////////////////////////
	//
	// CONFIGURATION INTERFACE
	//
	///////////////////////////
		
//	public UserAgentHeader getUserAgentHeader() {
//		return userAgent;
//	}
	
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


	public String getProxyAddress() {
		return proxyAddress;
	}


	public void setProxyAddress(String proxyAddress) {
		this.proxyAddress = proxyAddress;
	}


	public int getProxyPort() {
		return proxyPort;
	}


	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}


	public String getTransport() {
		return transport;
	}


	public void setTransport(String transport) {
		this.transport = transport;
	}
	
	public void setMaxForwards(int maxForwards){
		this.maxForwards = maxForwards;
	}
	
	public int getMaxForwards() {
		return maxForwards;
	}

	/////////////
	//
	// User manager interface
	//
	/////////////
	public SipEndPoint registerEndPoint(String user, String realm , int expires, SipEndPointListener handler) throws ParseException, ServerInternalErrorException {
		SipEndPointImpl epImpl;
		String epAddress = user+"@"+realm;
		if ((epImpl = endPoints.get(epAddress)) != null) {
			return epImpl;
		}
				
		epImpl = new SipEndPointImpl(user,realm,expires, this, handler);
		endPoints.put(epAddress,epImpl);
		return epImpl;
	}
	
	private void sendStateless(int code, Request request) {
		try {
			sipProvider.sendResponse(UaFactory.getMessageFactory().createResponse(code, request));
		} catch (Exception e) {
			log.error("UA: Unable to send stateless response code:" + code + ". GIVE UP!!!", e);
		}
	}

}
