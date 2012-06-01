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
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManager;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.sip.agent.SipContext;
import com.kurento.commons.sip.agent.SipEndPointImpl;

public abstract class Transaction {

	private static Logger log = LoggerFactory.getLogger(Transaction.class);

	protected String method;

	protected Dialog dialog;
	protected SipContext sipContext;

	protected SipEndPointImpl localParty;
	protected Address remoteParty;

	protected String localTag;
	protected static long cSeqNumber = System.currentTimeMillis() % 100000000;

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

//	public void processTimeOut(TimeoutEvent timeoutEvent) {
//		String msg ="Time Out while waiting for an ACK";
//		timeoutEvent.
//		log.error(msg);
//		if (sipContext != null)
//			sipContext.failedCall(msg);
//	}

}