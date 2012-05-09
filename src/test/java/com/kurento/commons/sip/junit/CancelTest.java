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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * RFC 3261 Chapter 9. Canceling a Request.
 */
public class CancelTest {

	private final static Logger log = LoggerFactory.getLogger(CancelTest.class);

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
	private static int expires = 120;
	private static String localAddress;

	private static EndPoint serverEndPoint;
	private static EndPoint clientEndPoint;

	@BeforeClass
	public static void initTest() throws Exception {

		log.info("Initialice SIP UA Cancel test");

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
				"", expires, serverUa, serverEndPointController, serverTimer,
				false);
		// Create SIP stack and activate SIP EndPoints
		serverUa.reconfigure();

		SipConfig sConfig = new SipConfig();
		sConfig.setProxyAddress(TestConfig.CLIENT_IP);
		sConfig.setProxyPort(TestConfig.CLIENT_PORT);
		sConfig.setLocalPort(TestConfig.PROXY_PORT);
		sConfig.setLocalAddress("lo0");

		clientUa = UaFactory.getInstance(sConfig);
		clientEndPointController = new SipEndPointController("client");
		clientTimer = new TestTimer();
		clientEndPoint = EndPointFactory.getInstance(clientName, "kurento.com",
				"", 10, clientUa, clientEndPointController, clientTimer, false);
		// Create SIP stack and activate SIP EndPoints
		clientUa.reconfigure();

	}

	@AfterClass
	public static void tearDown() {
		if (serverUa != null)
			serverUa.terminate();
		if (clientUa != null)
			clientUa.terminate();
	}

	/**
	 * <pre>
	 * C:-----INVITE-------------->:S
	 * C:<----------- 100 Trying
	 * C:<----------- 180 Ringing
	 * C:----CANCEL -------------->:S
	 * C:<----------- 487 Request Terminated
	 * C:----ACK------------------>:S
	 * C:<------------ 200 OK (CSeq: xxx CANCEL)
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCancel() throws Exception {
		log.info("-------------------- Test Cancel --------------------");

		EndPointEvent endPointEvent;
		CallEvent callEvent;

		// C:-----INVITE-------------->:S
		log.info(clientName + " dial to " + serverName + "...");
		SipCallController callControllerA1 = new SipCallController(clientName);
		Call initialCallA1 = clientEndPoint.dial(serverUri, callControllerA1);
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
		SipCallController callControllerB1 = new SipCallController(serverName);
		serverCall.addListener(callControllerB1);
		log.info("OK");

		// C:----CANCEL -------------->:S
		log.info(clientName + " cancel call...");
		initialCallA1.cancel();
		log.info("OK");

		log.info(serverName + " expects cancel from " + clientName + "...");
		callEvent = callControllerB1.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		assertTrue("Bad message received in server UA",
				CallEvent.CALL_CANCEL.equals(callEvent.getEventType()));
		log.info("OK");

		log.info(" -------------------- Test Cancel finished OK --------------------");

	}

	/**
	 * <pre>
	 * C:----INVITE-------------------------->:S
	 * C:<------------------------ 100 Trying
	 * C:<------------------------ 180 Ringing
	 * C:<------ 200 OK (CSeq: xxx INVITE)----:S
	 * C:----CANCEL-------------------------->:S
	 * C:----ACK ---------------------------->:S
	 * C:----BYE ---------------------------->:S
	 * C:<------ 200 OK (CSeq: xxx BYE)-------:S
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCancelAfterAccept() throws Exception {
		log.info("-------------------- Test Cancel After Accept --------------------");

		EndPointEvent endPointEvent;
		CallEvent callEvent;

		// C:-----INVITE-------------->:S
		log.info(clientName + " dial to " + serverName + "...");
		SipCallController callControllerA1 = new SipCallController(clientName);
		Call initialCallA1 = clientEndPoint.dial(serverUri, callControllerA1);
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
		SipCallController callControllerB1 = new SipCallController(serverName);
		serverCall.addListener(callControllerB1);
		log.info("OK");

		log.info(serverName + " accepts call...");
		serverCall.accept();
		log.info("OK");

		Thread.sleep(1000);

		log.info(clientName + " cancel call...");
		initialCallA1.cancel();
		log.info("OK");

		log.info(clientName + " expects call setup...");
		callEvent = callControllerA1.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		assertTrue("Bad message received in server UA",
				CallEvent.CALL_SETUP.equals(callEvent.getEventType()));
		log.info("OK");

		log.info(serverName + " expects call setup...");
		callEvent = callControllerB1.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		assertTrue("Bad message received in server UA",
				CallEvent.CALL_SETUP.equals(callEvent.getEventType()));
		log.info("OK");

		log.info(" -------------------- Test Cancel After Accept finished OK --------------------");
	}

	/**
	 * 
	 * <pre>
	 * C:INVITE-------------------------------------------->:S
	 * C:<----------- 100 Trying
	 * C:<----------- 180 Ringing
	 * C:CANCEL-----> X <------ 200 OK (CSeq: xxx INVITE)---:S
	 * C:ACK ---------------------------------------------->:S
	 * C:BYE ---------------------------------------------->:S
	 * C:<------------- 200 OK (CSeq: xxx BYE)--------------:S
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCancelBeforeAccept() throws Exception {
		log.info("-------------------- Test Cancel Before Accept --------------------");

		EndPointEvent endPointEvent;
		CallEvent callEvent;

		// C:-----INVITE-------------->:S
		log.info(clientName + " dial to " + serverName + "...");
		SipCallController callControllerA1 = new SipCallController(clientName);
		Call initialCallA1 = clientEndPoint.dial(serverUri, callControllerA1);
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
		SipCallController callControllerB1 = new SipCallController(serverName);
		serverCall.addListener(callControllerB1);
		log.info("OK");

		// C:------CANCEL----->:S
		log.info(clientName + " cancel call...");
		initialCallA1.cancel();
		log.info("OK");

		// C: <------ 200 OK (CSeq: xxx INVITE)---:S
		log.info(serverName + " accepts call...");
		serverCall.accept();
		log.info("OK");

		log.info(clientName + " expects call terminate...");
		callEvent = callControllerA1.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		assertTrue("Bad message received in server UA",
				CallEvent.CALL_TERMINATE.equals(callEvent.getEventType()));
		log.info("OK");

		// FIXME: try to make two different tests
		log.info(serverName + " expects CALL_SETUP or CALL_CANCEL...");
		callEvent = callControllerB1.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		if (CallEvent.CALL_SETUP.equals(callEvent.getEventType())) {
			log.info("CALL_SETUP received...");
			assertTrue("Bad message received in server UA",
					CallEvent.CALL_SETUP.equals(callEvent.getEventType()));
			log.info("OK");

			log.info(serverName + " expects call terminate...");
			callEvent = callControllerB1
					.pollSipEndPointEvent(TestConfig.WAIT_TIME);
			assertTrue("No message received in server UA", callEvent != null);
			assertTrue("Bad message received in server UA",
					CallEvent.CALL_TERMINATE.equals(callEvent.getEventType()));
		} else if (CallEvent.CALL_CANCEL.equals(callEvent.getEventType())) {
			log.info("CALL_CANCEL received...");
			assertTrue("Bad message received in server UA",
					CallEvent.CALL_CANCEL.equals(callEvent.getEventType()));
		}
		log.info("OK");

		log.info(" -------------------- Test Cancel Before Accept finished OK --------------------");
	}
}
