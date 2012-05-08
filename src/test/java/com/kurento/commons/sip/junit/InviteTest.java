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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;

import com.kurento.commons.sip.agent.EndPointFactory;
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

public class InviteTest {

	private final static Logger log = LoggerFactory
			.getLogger(RegisterTest.class);

	private static UA serverUa;
	private static UA clientUa;

	private static SipEndPointController serverEndPointController;
	private static SipEndPointController clientEndPointController;

	private static TestTimer serverTimer;
	private static TestTimer clientTimer;

	private static String domain = "kurento.com";
	private static String serverName = "server";
	private static String clientName = "client";
	private static String serverUri = "sip:" + serverName + "@" + domain;
	private static String clientUri = "sip:" + clientName + "@" + domain;

	private static EndPoint serverEndPoint;
	private static EndPoint clientEndPoint;

	@BeforeClass
	public static void initTest() throws Exception {

		log.info("Initialice SIP UA test");

		UaFactory.setMediaSession(new MediaSessionDummy());

		SipConfig cConfig = new SipConfig();
		cConfig.setProxyAddress(TestConfig.PROXY_IP);
		cConfig.setProxyPort(TestConfig.PROXY_PORT);
		cConfig.setLocalPort(TestConfig.CLIENT_PORT);
		cConfig.setLocalAddress("lo0");

		serverUa = UaFactory.getInstance(cConfig);
		serverEndPointController = new SipEndPointController(serverName);
		serverTimer = new TestTimer();
		// Create and register SIP EndPoint
		serverEndPoint = EndPointFactory.getInstance(serverName, "kurento.com",
				"", 10, serverUa, serverEndPointController, serverTimer, false);
		// Create SIP stack and activate SIP EndPoints
		serverUa.reconfigure();

		SipConfig sConfig = new SipConfig();
		sConfig.setProxyAddress(TestConfig.CLIENT_IP);
		sConfig.setProxyPort(TestConfig.CLIENT_PORT);
		sConfig.setLocalPort(TestConfig.PROXY_PORT);
		sConfig.setLocalAddress("lo0");

		clientUa = UaFactory.getInstance(sConfig);
		clientEndPointController = new SipEndPointController("client");
		serverTimer = new TestTimer();
		clientEndPoint = EndPointFactory.getInstance(clientName, "kurento.com",
				"", 10, clientUa, clientEndPointController, clientTimer, false);
		// Create SIP stack and activate SIP EndPoints
		clientUa.reconfigure();

	}

	@AfterClass
	public static void tearDown() {
		serverUa.terminate();
		clientUa.terminate();
	}

	/**
	 * C:---INVITE---------->:S<br>
	 * C:<----------200 OK---:S<br>
	 * C:---ACK------------->:S<br>
	 * C:---BYE------------->:S<br>
	 * C:<----------200 OK---:S<br>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCallSetupAndDropFromCaller() throws Exception {
		log.info("-------------------- Test Call Setup and Drop from caller --------------------");

		EndPointEvent endPointEvent;
		CallEvent callEvent;

		// C:---INVITE---------->:S
		log.info(clientName + " dial to " + serverName + "...");
		SipCallController callControllerClient = new SipCallController(
				clientName);
		Call clientCall = clientEndPoint.dial(serverUri, callControllerClient);
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

		// C:<----------200 OK --:S
		log.info(serverName + " accepts call...");
		SipCallController callControllerServer = new SipCallController(
				serverName);
		serverCall.addListener(callControllerServer);
		serverCall.accept();
		log.info("OK");

		log.info(clientName + " expects accepted call from " + serverName
				+ "...");
		callEvent = callControllerClient
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", callEvent != null);
		assertTrue("Bad message received in client UA",
				CallEvent.CALL_SETUP.equals(callEvent.getEventType()));
		log.info("OK");

		// C:---ACK------------->:S
		log.info(serverName + " expects ACK from " + clientName + "...");
		callEvent = callControllerServer
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", callEvent != null);
		assertTrue("Bad message received in client UA",
				CallEvent.CALL_SETUP.equals(callEvent.getEventType()));
		log.info("OK");

		// C:---BYE------------->:S
		log.info(clientName + " hangup...");
		clientCall.hangup();
		log.info("OK");

		log.info(serverName + " expects call hangup from " + clientName + "...");
		callEvent = callControllerServer
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		assertTrue("Bad message received in server UA",
				CallEvent.CALL_TERMINATE.equals(callEvent.getEventType()));
		log.info("OK");

		// C:<----------200 OK --:S
		log.info(clientName + " call terminate...");
		callEvent = callControllerClient
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", callEvent != null);
		assertTrue("Bad message received in client UA",
				CallEvent.CALL_TERMINATE.equals(callEvent.getEventType()));

		log.info("OK");

		log.info(" -------------------- Test Call Setup and Drop from caller finished OK --------------------");
	}

	// public void testCallSetupAndDropFromCalled() throws Exception {
	// log.info("-------------------- Test Call Setup and Drop from called --------------------");
	//
	// EndPointEvent endPointEvent;
	// CallEvent callEvent;
	// int expires = 3600;
	//
	//
	// EndPoint endpointA1;
	// String userA1 = TestConfig.USER + testConfig.getCounter();
	// SipEndPointController endPointControllerA1 = new
	// SipEndPointController(userA1);
	//
	// log.info("Register user " + userA1 + "...");
	// endpointA1 = userAgentA.registerEndPoint(userA1, TestConfig.DOMAIN,
	// TestConfig.PASS, expires, endPointControllerA1);
	// endPointEvent =
	// endPointControllerA1.pollSipEndPointEvent(TestConfig.WAIT_TIME);
	// assertNotNull(endPointEvent);
	// assertEquals(EndPointEvent.REGISTER_USER_SUCESSFUL,
	// endPointEvent.getEventType());
	// log.info("OK");
	//
	//
	// EndPoint endpointB1;
	// String userB1 = TestConfig.USER + testConfig.getCounter();
	// String userURIB1 = "sip:" + userB1 + "@" + TestConfig.DOMAIN;
	// SipEndPointController endPointControllerB1 = new
	// SipEndPointController(userB1);
	//
	// log.info("Register user " + userB1 + "...");
	// endpointB1 = userAgentA.registerEndPoint(userB1, TestConfig.DOMAIN,
	// TestConfig.PASS, expires, endPointControllerB1);
	// endPointEvent =
	// endPointControllerB1.pollSipEndPointEvent(TestConfig.WAIT_TIME);
	// assertNotNull(endPointEvent);
	// assertEquals(EndPointEvent.REGISTER_USER_SUCESSFUL,
	// endPointEvent.getEventType());
	// log.info("OK");
	//
	// log.info(userA1 + " dial to " + userB1 + "...");
	// SipCallController callControllerA1 = new SipCallController(userA1);
	// Call initialCallA1 = endpointA1.dial(userURIB1, callControllerA1);
	// log.info("OK");
	//
	// log.info(userB1 + " expects incoming call from " + userA1 + "...");
	// endPointEvent =
	// endPointControllerB1.pollSipEndPointEvent(TestConfig.WAIT_TIME);
	// assertNotNull(endPointEvent);
	// assertEquals(EndPointEvent.INCOMING_CALL, endPointEvent.getEventType());
	// Call receivedCallB1 = endPointEvent.getCallSource();
	// log.info("OK");
	//
	// log.info(userB1 + " accepts call...");
	// SipCallController callControllerB1 = new SipCallController(userB1);
	// receivedCallB1.addListener(callControllerB1);
	// receivedCallB1.accept();
	// log.info("OK");
	//
	// log.info(userA1 + " expects accepted call from " + userB1 + "...");
	// callEvent = callControllerA1.pollSipEndPointEvent(TestConfig.WAIT_TIME);
	// assertNotNull(callEvent);
	// assertEquals(CallEvent.CALL_SETUP, callEvent.getEventType());
	// log.info("OK");
	//
	// log.info(userB1 + " expects ACK from " + userA1 + "...");
	// callEvent = callControllerB1.pollSipEndPointEvent(TestConfig.WAIT_TIME);
	// assertNotNull(callEvent);
	// assertEquals(CallEvent.CALL_SETUP, callEvent.getEventType());
	// log.info("OK");
	//
	// log.info(userB1 + " hangup...");
	// receivedCallB1.hangup();
	// log.info("OK");
	//
	// log.info(userA1 + " expects call hangup from " + userB1 + "...");
	// callEvent = callControllerB1.pollSipEndPointEvent(TestConfig.WAIT_TIME);
	// assertNotNull(callEvent);
	// assertEquals(CallEvent.CALL_TERMINATE, callEvent.getEventType());
	// log.info("OK");
	//
	// log.info(userB1 + " call terminate...");
	// callEvent = callControllerA1.pollSipEndPointEvent(TestConfig.WAIT_TIME);
	// assertNotNull(callEvent);
	// assertEquals(CallEvent.CALL_TERMINATE, callEvent.getEventType());
	// log.info("OK");
	//
	// log.info(" -------------------- Test Call Setup and Drop from called finished OK --------------------");
	// }

}
