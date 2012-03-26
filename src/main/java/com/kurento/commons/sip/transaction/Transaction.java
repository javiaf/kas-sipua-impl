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
package com.kurento.commons.sip.transaction;

import javax.sdp.SessionDescription;
import javax.sip.Dialog;
import javax.sip.TimeoutEvent;
import javax.sip.address.Address;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.mscontrol.MediaEventListener;
import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManager;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.sip.agent.SipContext;
import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public abstract class Transaction implements
		MediaEventListener<SdpPortManagerEvent> {

	private static Logger log = LoggerFactory.getLogger(Transaction.class);

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
		log.info("Process received SDP answer");
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