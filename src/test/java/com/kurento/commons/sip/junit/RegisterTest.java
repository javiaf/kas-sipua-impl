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
import com.kurento.commons.sip.agent.UaImpl;
import com.kurento.commons.sip.testutils.MediaSessionDummy;
import com.kurento.commons.sip.testutils.SipEndPointController;
import com.kurento.commons.sip.testutils.TestConfig;
import com.kurento.commons.sip.testutils.TestTimer;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.UA;
import com.kurento.commons.ua.event.EndPointEvent;

public class RegisterTest {

	private final static Logger log = LoggerFactory
			.getLogger(RegisterTest.class);

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
	private static int expires = 6;
	private static String localAddress;

	private static EndPoint serverEndPoint;
	private static EndPoint clientEndPoint;

	@BeforeClass
	public static void initTest() throws Exception {

		if (System.getProperty("os.name").startsWith("Mac"))
			localAddress = "lo0";
		else
			localAddress = "lo";

		log.info("Initialice SIP UA for register tests in platform: "
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
		serverEndPoint = EndPointFactory.getInstance(serverName, "kurento.com",
				"", expires, serverUa, serverEndPointController, false);
		// Create SIP stack and activate SIP EndPoints
		serverUa.reconfigure();

	}

	@AfterClass
	public static void tearDown() {
		serverUa.terminate();
	}

	/**
	 * Verify the EndPoint is able to register a contact for a given SIPURI and
	 * it manages its register/non-register status
	 * 
	 * <pre>
	 *  1 - C:---REGISTER----------->:S
	 *      C:<-------------200 OK---:S
	 *  2 - C:sipEndPoint.terminate()
	 *      C:---REGISTER(exp=0)---->:S
	 *      C:<-------------200 OK---:S
	 *  3 - C:sipEndPoint.terminate()
	 *      C:---REGISTER---x        :S : Verify no REGISTER request is sent (EP is already un-register)
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRegisterAndUnregister() throws Exception {
		log.info("-------------------- Test Register and Un-register --------------------");

		// C:---REGISTER-------->:S
		log.info("Register user " + clientName + "...");

		SipConfig cConfig = new SipConfig();
		cConfig.setProxyAddress(TestConfig.PROXY_IP);
		cConfig.setProxyPort(TestConfig.PROXY_PORT);
		cConfig.setLocalPort(TestConfig.CLIENT_PORT);
		cConfig.setLocalAddress(localAddress);
		cConfig.setTimer(timer);

		clientUa = UaFactory.getInstance(cConfig);
		clientEndPointController = new SipEndPointController(clientName);
		clientEndPoint = EndPointFactory.getInstance(clientName, "kurento.com",
				"", expires, clientUa, clientEndPointController, true);
		// Create SIP stack and activate SIP EndPoints
		clientUa.reconfigure();

		// TODO: Unable to monitor message reception on server side

		// C:<----------200 OK---:S
		EndPointEvent endPointEvent = clientEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);

		assertTrue("No message received in client UA", endPointEvent != null);
		assertTrue(
				"Bad message received in client UA: "
						+ endPointEvent.getEventType(),
				EndPointEvent.REGISTER_USER_SUCESSFUL.equals(endPointEvent
						.getEventType()));
		log.info("OK");

		// C:---REGISTER(exp=0)->:S
		log.info("Implicit un-reregister of user " + clientName + "...");
		clientEndPoint.terminate();

		// C:<----------200 OK---:S
		endPointEvent = clientEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", endPointEvent != null);
		assertTrue(
				"Bad message received in client UA: "
						+ endPointEvent.getEventType(),
				EndPointEvent.REGISTER_USER_SUCESSFUL.equals(endPointEvent
						.getEventType()));
		log.info("OK");

		// C:---REGISTER(exp=0)->:S (second time)
		log.info("Terminate user " + clientName
				+ "... Verify no register request is sent");
		clientEndPoint.terminate();
		endPointEvent = clientEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("Client UA sent unregister twice", endPointEvent == null);

		// Check no register is sent when UA terminates
		clientUa.terminate();
		endPointEvent = clientEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("Client UA sent unregister twice", endPointEvent == null);

		log.info(" -------------------- Test Register and Un-register finished OK --------------------");

	}

	// TODO: This test will require an event sent by the SRegister transaction
	// to the UA in order to verify the realm and decide to accept or not the
	// register request

	// public void testRegisterFail() throws Exception {
	// log.info("-------------------- Test Register Fail --------------------");
	//
	// int expires = 3600;
	//
	// String user = TestConfig.USER + testConfig.getCounter();
	// SipEndPointController registerController = new
	// SipEndPointController(user);
	//
	// log.info("Register user " + user +
	// " with invalid domain to expect register fail.");
	// EndPoint endpoint = userAgent.registerEndPoint(user,
	// TestConfig.INVALID_DOMAIN, "none", expires, registerController);
	// EndPointEvent event =
	// registerController.pollSipEndPointEvent(TestConfig.WAIT_TIME);
	// assertNotNull(event);
	// assertEquals(EndPointEvent.REGISTER_USER_FAIL, event.getEventType());
	// log.info("OK");
	//
	// log.info(" -------------------- Test Register Fail finished OK --------------------");
	// }
	//

	/**
	 * Verify the UA sends REGISTER request before expiration time, while the
	 * SIPURI contact is registered. After a message with expires set to 0
	 * REGISTER messages must stop
	 * 
	 * <pre>
	 * C:---REGISTER-------->:S
	 * C:<----------200 OK---:S
	 * C:---REGISTER-------->:S (Before expires)
	 * C:<----------200 OK---:S
	 * C:---REGISTER-------->:S (Before expires)
	 * C:<----------200 OK---:S
	 * ...
	 * C:sipEndPoint.terminate()
	 * C:---REGISTER(exp=0)->:S
	 * C:<----------200 OK-->:S
	 * C:---REGISTER---x     :S : Verify no REGISTER request is sent
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRegisterKeepAlive() throws Exception {

		log.info("-------------------- Test Register KeepAlive --------------------");

		// C:---REGISTER-------->:S
		log.info("Register user " + clientName + "...");

		SipConfig cConfig = new SipConfig();
		cConfig.setProxyAddress(TestConfig.PROXY_IP);
		cConfig.setProxyPort(TestConfig.PROXY_PORT);
		cConfig.setLocalPort(TestConfig.CLIENT_PORT);
		cConfig.setLocalAddress(localAddress);
		cConfig.setTimer(timer);

		clientUa = UaFactory.getInstance(cConfig);
		clientEndPointController = new SipEndPointController(clientName);
		clientEndPoint = EndPointFactory.getInstance(clientName, "kurento.com",
				"", expires, clientUa, clientEndPointController, true);
		// Create SIP stack and activate SIP EndPoints
		clientUa.reconfigure();
		long tStart = System.currentTimeMillis();

		// TODO: Unable to monitor message reception on server side

		EndPointEvent endPointEvent = clientEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", endPointEvent != null);
		assertTrue(
				"Bad message received in client UA: "
						+ endPointEvent.getEventType(),
				EndPointEvent.REGISTER_USER_SUCESSFUL.equals(endPointEvent
						.getEventType()));
		log.info("OK");

		// C:---REGISTER-------->:S (Before expires)
		// C:<----------200 OK---:S
		// C:---REGISTER-------->:S (Before expires)
		// C:<----------200 OK---:S
		// ...
		int i;
		for (i = 0; i < 5; i++) {
			log.info("Wait for register keep alive of user " + clientName
					+ "...");
			endPointEvent = clientEndPointController
					.pollSipEndPointEvent(expires);
			assertTrue("No message received in client UA",
					endPointEvent != null);
			assertTrue(
					"Bad message received in client UA: "
							+ endPointEvent.getEventType(),
					EndPointEvent.REGISTER_USER_SUCESSFUL.equals(endPointEvent
							.getEventType()));
			log.info("----> Register keep-alive: " + i + " after "
					+ (System.currentTimeMillis() - tStart) + " ms");
		}

		log.info("Deregister user " + clientName + "...");
		clientEndPoint.terminate();
		endPointEvent = clientEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", endPointEvent != null);
		assertTrue(
				"Bad message received in client UA: "
						+ endPointEvent.getEventType(),
				EndPointEvent.REGISTER_USER_SUCESSFUL.equals(endPointEvent
						.getEventType()));

		log.info("OK");

		endPointEvent = clientEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", endPointEvent == null);
		log.info("REGISTER keep-alive sent after EP termination");

		clientUa.terminate();

		log.info(" -------------------- Test Register KeepAlive finished OK --------------------");
	}

	/**
	 * Verify the EndPoint handles REGISTER timeouts due to connection problems
	 * 
	 * <pre>
	 * C:---REGISTER-------->:S
	 * C:   x------TIMEOUT---:S
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRegisterTimeOut() throws Exception {
		log.info("-------------------- Test Register Timeout --------------------");

		// C:---REGISTER-------->:S
		log.info("Register user " + clientName + "...");

		SipConfig cConfig = new SipConfig();
		cConfig.setProxyAddress(TestConfig.PROXY_IP);
		cConfig.setProxyPort(TestConfig.PROXY_PORT + 1);
		cConfig.setLocalPort(TestConfig.CLIENT_PORT);
		cConfig.setLocalAddress(localAddress);
		cConfig.setTimer(timer);

		clientUa = UaFactory.getInstance(cConfig);
		clientEndPointController = new SipEndPointController("client");
		clientEndPoint = EndPointFactory.getInstance(clientName, "kurento.com",
				"", expires, clientUa, clientEndPointController, true);
		// Create SIP stack and activate SIP EndPoints
		clientUa.reconfigure();

		// C: x------TIMEOUT---:S
		EndPointEvent endPointEvent = clientEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME * 10);
		assertTrue("No message received in client UA", endPointEvent != null);
		assertTrue(
				"Bad message received in client UA: "
						+ endPointEvent.getEventType(),
				EndPointEvent.REGISTER_USER_FAIL.equals(endPointEvent
						.getEventType()));
		log.info("OK");

	}

	/**
	 * Verify the User Agent properly handles call and sip protocol after lost
	 * of network connection
	 * 
	 * <pre>
	 * C:---REGISTER-------->:S
	 * C:<----------200 OK---:S
	 * ...
	 * C:Network Interface change
	 * ...
	 * C:---REGISTER(exp=0)->:S - Un-register old contact
	 * C:<----------200 OK-->:S
	 * C:---REGISTER-------->:S - Register new contact
	 * C:<----------200 OK---:S
	 * </pre>
	 * @throws Exception 
	 */
	@Test
	public void testRegisterAfterNetworkInterfaceChange() throws Exception {

		log.info("-------------------- Test Register after network interface change --------------------");

		// C:---REGISTER-------->:S
		log.info("Register user " + clientName + "...");

		SipConfig cConfig = new SipConfig();
		cConfig.setProxyAddress(TestConfig.PROXY_IP);
		cConfig.setProxyPort(TestConfig.PROXY_PORT);
		cConfig.setLocalPort(TestConfig.CLIENT_PORT);
		cConfig.setLocalAddress(localAddress);
		cConfig.setTimer(timer);

		clientUa = UaFactory.getInstance(cConfig);
		((UaImpl) clientUa).setTestMode(true);
		clientEndPointController = new SipEndPointController("client");
		clientEndPoint = EndPointFactory.getInstance(clientName, "kurento.com",
				"", expires, clientUa, clientEndPointController, true);
		// Create SIP stack and activate SIP EndPoints
		clientUa.reconfigure();
		
		// Check register is successfully completed
		EndPointEvent endPointEvent = clientEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", endPointEvent != null);
		assertTrue(
				"Bad message received in client UA: "
						+ endPointEvent.getEventType(),
				EndPointEvent.REGISTER_USER_SUCESSFUL.equals(endPointEvent
						.getEventType()));
		log.info("OK");
		
		// Emulate network change with the UA reconfiguration 
		log.info(clientName + " has changed network interface" );
		clientUa.reconfigure();
		
		// Check un-register is successfully completed
		// TODO: add special event to signal unregister
		endPointEvent = clientEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", endPointEvent != null);
		assertTrue(
				"Bad message received in client UA: "
						+ endPointEvent.getEventType(),
				EndPointEvent.REGISTER_USER_SUCESSFUL.equals(endPointEvent
						.getEventType()));
		log.info("OK");

		// Check un-register is successfully completed
		endPointEvent = clientEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", endPointEvent != null);
		assertTrue(
				"Bad message received in client UA: "
						+ endPointEvent.getEventType(),
				EndPointEvent.REGISTER_USER_SUCESSFUL.equals(endPointEvent
						.getEventType()));
		log.info("OK");

	}

}
