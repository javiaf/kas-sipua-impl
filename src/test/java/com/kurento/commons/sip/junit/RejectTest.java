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
package com.kurento.commons.sip.junit;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.kurento.commons.sip.agent.EndPointFactory;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.testutils.MediaSessionDummy;
import com.kurento.commons.sip.testutils.SipCallController;
import com.kurento.commons.sip.testutils.SipEndPointController;
import com.kurento.commons.sip.testutils.TestConfig;
import com.kurento.commons.sip.testutils.TestTimer;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commons.ua.Call;
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.UA;
import com.kurento.commons.ua.event.CallEvent;
import com.kurento.commons.ua.event.EndPointEvent;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public class RejectTest {

	private final static Logger log = LoggerFactory.getLogger(RejectTest.class);

	private static UA serverUa;
	private static UA clientUa;

	private static SipEndPointController serverEndPointController;
	private static SipEndPointController clientEndPointController;

	private static TestTimer timer;

	private static String domain = "kurento.com";
	private static String serverName = "server";
	private static String clientName = "client";
	private static String serverUri = "sip:" + serverName + "@" + domain;
	private static String clientUri = "sip:" + clientName + "@" + domain;
	private static int expires = 1000;
	private static String localAddress;

	private static EndPoint serverEndPoint;
	private static EndPoint clientEndPoint;

	@BeforeClass
	public static void initTest() throws Exception {

		if (System.getProperty("os.name").startsWith("Mac"))
			localAddress = "lo0";
		else
			localAddress = "lo";

		log.info("Initialice SIP UA for register tests in platform:"
				+ System.getProperty("os.name"));

		UaFactory.setMediaSession(new MediaSessionDummy());

		timer = new TestTimer();

		SipConfig sConfig = new SipConfig();
		sConfig.setProxyAddress(TestConfig.CLIENT_IP);
		sConfig.setProxyPort(TestConfig.CLIENT_PORT);
		sConfig.setLocalPort(TestConfig.PROXY_PORT);
		sConfig.setLocalAddress(localAddress);
		sConfig.setTimer(timer);

		serverUa = UaFactory.getInstance(sConfig);
		serverEndPointController = new SipEndPointController(serverName);
		// Create and register SIP EndPoint
		Map<String, Object> sEpConfig = new HashMap<String, Object>();
		sEpConfig.put("SIP_RECEIVE_CALL", false);
		serverEndPoint = serverUa.registerEndpoint(serverName, "kurento.com",
				serverEndPointController, sEpConfig);

		// serverEndPoint = EndPointFactory.getInstance(serverName,
		// "kurento.com",
		// "", expires, serverUa, serverEndPointController, false);
		// Create SIP stack and activate SIP EndPoints
		serverUa.reconfigure();

		log.info("	ServerUa created");

		SipConfig cConfig = new SipConfig();
		cConfig.setProxyAddress(TestConfig.PROXY_IP);
		cConfig.setProxyPort(TestConfig.PROXY_PORT);
		cConfig.setLocalPort(TestConfig.CLIENT_PORT);
		cConfig.setLocalAddress(localAddress);
		cConfig.setTimer(timer);

		clientUa = UaFactory.getInstance(cConfig);
		clientEndPointController = new SipEndPointController(clientName);
		Map<String, Object> cEpConfig = new HashMap<String, Object>();
		cEpConfig.put("SIP_RECEIVE_CALL", false);
		clientEndPoint = clientUa.registerEndpoint(clientName, "kurento.com",
				clientEndPointController, cEpConfig);
		// clientEndPoint = EndPointFactory.getInstance(clientName,
		// "kurento.com",
		// "", expires, clientUa, clientEndPointController, false);
		// Create SIP stack and activate SIP EndPoints
		clientUa.reconfigure();

		log.info("	ClientUa created");

	}

	@AfterClass
	public static void tearDown() {
		serverUa.terminate();
		clientUa.terminate();
	}

	/**
	 * Verify call is terminated in both sides after the caller party rejects
	 * connection request
	 * 
	 * <pre>
	 * C:---INVITE---------->:S
	 * C:<----------REJECT---:S
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRejectCall() throws Exception {
		log.info("-------------------- Test Invite with Reject  --------------------");

		EndPointEvent endPointEvent;
		CallEvent callEvent;

		// C:---INVITE---------->:S
		log.info(clientName + " dial to " + serverName + "...");
		SipCallController clientCallController = new SipCallController(
				clientName);
		clientEndPoint.dial(serverUri, clientCallController);
		log.info("OK");

		log.info(serverName + " expects incoming call from " + clientName
				+ "...");
		endPointEvent = serverEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", endPointEvent != null);
		assertTrue(
				"Bad message received in server UA: "
						+ endPointEvent.getEventType(),
				EndPointEvent.INCOMING_CALL.equals(endPointEvent.getEventType()));

		Call serverCall = endPointEvent.getCallSource();
		log.info("OK");

		// C:<------REJECT-------:S
		log.info(serverName + " rejects call...");
		SipCallController serverCallController = new SipCallController(serverName);
		serverCall.addListener(serverCallController);
		serverCall.hangup();
		log.info("OK");

		log.info(clientName + " expects call rejected from " + serverName
				+ "...");
		callEvent = clientCallController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", callEvent != null);
		assertTrue("Bad message received in client UA",
				CallEvent.CALL_REJECT.equals(callEvent.getEventType()));
		log.info("OK");

		log.info("-------------------- Test Invite with Reject finished OK --------------------");

	}

	/**
	 * Verify call is terminated in both sides with reject code when CANCEL
	 * request is simultaneously sent and received after reject response is sent
	 * 
	 * <pre>
	 * C:---INVITE------------------->:S
	 * C:--- CANCEL ---X<----REJECT---:S
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRejectCallWithSimultaneousCancel() throws Exception {
		log.info("-------------------- Test Invite with Reject and simultaneous CANCEL --------------------");

		EndPointEvent endPointEvent;
		CallEvent callEvent;

		// C:---INVITE---------->:S
		log.info(clientName + " dial to " + serverName + "...");
		SipCallController clientCallController = new SipCallController(
				clientName);
		Call clientCall = clientEndPoint.dial(serverUri, clientCallController);
		log.info("OK");

		log.info(serverName + " expects incoming call from " + clientName
				+ "...");
		endPointEvent = serverEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", endPointEvent != null);
		assertTrue(
				"Bad message received in server UA: "
						+ endPointEvent.getEventType(),
				EndPointEvent.INCOMING_CALL.equals(endPointEvent.getEventType()));

		final Call serverCallFinal = endPointEvent.getCallSource();
		log.info("OK");

		// C:<------REJECT-------:S
		// Reject call in a new thread to enable simultaneous CANCEL
		log.info(serverName + " rejects call...");
		SipCallController serverCallController = new SipCallController(serverName);
		serverCallFinal.addListener(serverCallController);
		new Thread(new Runnable() {
			public void run() {
				try {	
					serverCallFinal.hangup();
				} catch (ServerInternalErrorException e) {
					log.error("Unable to accept call", e);
				}
			}
		}).start();
		log.info("OK");

		// Send cancel request from Client
		// C:--- CANCEL ---X
		clientCall.hangup();
		
		// Server controller expects call rejected
		log.info(serverName + " expects call rejected");
		callEvent = serverCallController.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", callEvent != null);
		assertTrue("Bad message received in client UA",
				CallEvent.CALL_REJECT.equals(callEvent.getEventType()));
		log.info("OK");

		// Server controller expects call terminated
		log.info(serverName + " expects call terminated");
		callEvent = serverCallController.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", callEvent != null);
		assertTrue("Bad message received in client UA",
				CallEvent.CALL_TERMINATE.equals(callEvent.getEventType()));
		log.info("OK");

		
		// Client controller expects call rejected
		log.info(clientName + " expects call rejected from " + serverName
				+ "...");
		callEvent = clientCallController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", callEvent != null);
		assertTrue("Bad message received in client UA",
				CallEvent.CALL_REJECT.equals(callEvent.getEventType()));
		log.info("OK");

		
		// Client controller expects call terminated
		log.info(clientName + " expects call termianted ...");
		callEvent = clientCallController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", callEvent != null);
		assertTrue("Bad message received in client UA",
				CallEvent.CALL_TERMINATE.equals(callEvent.getEventType()));
		log.info("OK");		
	
		log.info("-------------------- Test Invite with Reject and simultaneous CANCEL finished OK --------------------");
	}

}
