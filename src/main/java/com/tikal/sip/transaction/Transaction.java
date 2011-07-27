package com.tikal.sip.transaction;

import javax.sdp.SessionDescription;
import javax.sip.Dialog;
import javax.sip.TimeoutEvent;
import javax.sip.address.Address;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tikal.mscontrol.MsControlException;
import com.tikal.mscontrol.MediaEventListener;
import com.tikal.mscontrol.networkconnection.NetworkConnection;
import com.tikal.mscontrol.networkconnection.SdpPortManager;
import com.tikal.mscontrol.networkconnection.SdpPortManagerEvent;
import com.tikal.sip.agent.SipContext;
import com.tikal.sip.agent.SipEndPointImpl;
import com.tikal.sip.agent.UaFactory;
import com.tikal.sip.exception.ServerInternalErrorException;

public abstract class Transaction implements
		MediaEventListener<SdpPortManagerEvent> {

	private Log log = LogFactory.getLog(Transaction.class);

	protected String method;

	protected Dialog dialog;
	protected SipContext sipContext;

	protected SipEndPointImpl localParty;
	protected Address remoteParty;

	protected String localTag;
	protected static long cSeqNumber = System.currentTimeMillis() % 100000000;

	protected NetworkConnection networkConnection;
	protected SdpPortManager sdpPortManager;
	protected SessionDescription localSdp;
	protected byte[] remoteSdp;

	// ///////////////
	//
	// Constructor
	//
	// ///////////////

	protected Transaction(String method, SipEndPointImpl localParty,
			Address remoteParty) {
		this.method = method;
		this.remoteParty = remoteParty;
		this.localParty = localParty;
	}

	protected void generateSdp() throws ServerInternalErrorException {
		// Request SDP
		log.debug("Request SDP template to SdpPortManager");
		try {
			createSdpPortManager();
			sdpPortManager.addListener(this);
			sdpPortManager.generateSdpOffer();
		} catch (MsControlException e) {
			throw new ServerInternalErrorException(
					"SDP negociation error while generating offer", e);
		}

	}

	protected void processSdpOffer(byte[] rawSdp)
			throws ServerInternalErrorException {
		log.debug("Process received SDP offer");
		remoteSdp = rawSdp;
		try {
			createSdpPortManager();
			sdpPortManager.addListener(this);
			sdpPortManager.processSdpOffer(rawSdp);
		} catch (MsControlException e) {
			throw new ServerInternalErrorException(
					"SDP negociation error while processing answer", e);

		}
	}

	protected void processSdpAnswer(byte[] rawSdp)
			throws ServerInternalErrorException {
		log.debug("Process received SDP answer");
		remoteSdp = rawSdp;
		try {
			createSdpPortManager();
			sdpPortManager.addListener(this);
			sdpPortManager.processSdpAnswer(rawSdp);
		} catch (MsControlException e) {
			throw new ServerInternalErrorException(
					"SDP negociation error while processing answer", e);
		}
	}

	private void createSdpPortManager() throws MsControlException,
			ServerInternalErrorException {
		if (networkConnection == null) {
			networkConnection = UaFactory.getMediaSession().createNetworkConnection();
			sdpPortManager = networkConnection.getSdpPortManager();
		}

	}

	protected void release() {
		if (networkConnection != null) {
			networkConnection.release();
		}
	}

	public SessionDescription getLocalSdp() {
		return localSdp;
	}

	public NetworkConnection getNetworkConnection() {
		return networkConnection;
	}

	public void processTimeOut(TimeoutEvent timeoutEvent) {
		// This function avoids every transaction to define processTimeOut
		log.error("Time Out while waiting a response from client transaction: "
				+ method);
		release();
	}

}