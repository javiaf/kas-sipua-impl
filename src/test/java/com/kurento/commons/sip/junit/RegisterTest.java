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
import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.testutils.MediaSessionDummy;
import com.kurento.commons.sip.testutils.SipCallController;
import com.kurento.commons.sip.testutils.SipEndPointController;
import com.kurento.commons.sip.testutils.TestConfig;
import com.kurento.commons.sip.testutils.TestTimer;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.UA;
import com.kurento.commons.ua.event.EndPointEvent;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public class RegisterTest  {

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
	private static int expires = 5;

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
				"", expires, serverUa, serverEndPointController, serverTimer, false);
		// Create SIP stack and activate SIP EndPoints
		serverUa.reconfigure();

	}

	/**
	 * <pre>
	 * C:---REGISTER-------->:S
	 * C:<----------200 OK---:S
	 * C:---REGISTER(exp=0)->:S
	 * C:<----------200 OK-->:S
	 * C:sipEndPoint.terminate()
	 * C:---REGISTER---x     :S : Verify no REGISTER request is sent (EP is already un-register)
	 * </pre>
	 * @throws Exception
	 */
	@Test
	public void testRegisterSuccesfull() throws Exception {
		log.info("-------------------- Test Register Succesfull --------------------");

		// C:---REGISTER-------->:S
		log.info("Register user " + clientName + "...");
		
		SipConfig sConfig = new SipConfig();
		sConfig.setProxyAddress(TestConfig.CLIENT_IP);
		sConfig.setProxyPort(TestConfig.CLIENT_PORT);
		sConfig.setLocalPort(TestConfig.PROXY_PORT);
		sConfig.setLocalAddress("lo0");

		clientUa = UaFactory.getInstance(sConfig);
		clientEndPointController = new SipEndPointController("client");
		clientTimer = new TestTimer();
		clientEndPoint = EndPointFactory.getInstance(clientName, "kurento.com",
				"", expires, clientUa, clientEndPointController, clientTimer, true);
		// Create SIP stack and activate SIP EndPoints
		clientUa.reconfigure();

		// TODO: Unable to monitor message reception on server side
		
		// C:<----------200 OK---:S
		EndPointEvent endPointEvent = clientEndPointController.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		
		assertTrue("No message received in client UA", endPointEvent != null);
		assertTrue("Bad message received in client UA: "
						+ endPointEvent.getEventType(),
				EndPointEvent.REGISTER_USER_SUCESSFUL.equals(endPointEvent.getEventType()));
		log.info("OK");

		log.info("Deregister user " + clientName + "...");
		clientEndPoint.terminate();
		endPointEvent = clientEndPointController.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", endPointEvent != null);
		assertTrue("Bad message received in client UA: "
						+ endPointEvent.getEventType(),
				EndPointEvent.REGISTER_USER_SUCESSFUL.equals(endPointEvent.getEventType()));
		log.info("OK");
				
		// Check no further unregister messages are sent
		log.info("Terminate user " + clientName + "... Verify no register request is sent");
		clientEndPoint.terminate();
		endPointEvent = clientEndPointController.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("Client UA sent unregister twice", endPointEvent == null);

		log.info(" -------------------- Test Register Succesfull finished OK --------------------");
	}


//	public void testRegisterFail() throws Exception {
//		log.info("-------------------- Test Register Fail --------------------");
//
//		int expires = 3600;
//
//		String user = TestConfig.USER + testConfig.getCounter();
//		SipEndPointController registerController = new SipEndPointController(user);
//		
//		log.info("Register user " + user + " with invalid domain to expect register fail.");
//		EndPoint endpoint = userAgent.registerEndPoint(user,
//				TestConfig.INVALID_DOMAIN, "none", expires, registerController);
//		EndPointEvent event = registerController.pollSipEndPointEvent(TestConfig.WAIT_TIME);
//		assertNotNull(event);
//		assertEquals(EndPointEvent.REGISTER_USER_FAIL, event.getEventType());
//		log.info("OK");
//
//		log.info(" -------------------- Test Register Fail finished OK --------------------");
//	}
//
//
//	public void testRegisterKeepAlive() throws Exception {
//		log.info("-------------------- Test Register KeepAlive --------------------");
//
//		EndPoint endpoint;
//		EndPointEvent event;
//		int expires = 5;
//		long tStart, tEnd;
//
//		String user = TestConfig.USER + testConfig.getCounter();
//		SipEndPointController registerController = new SipEndPointController(user);
//
//		tStart = System.currentTimeMillis();
//
//		log.info("Register user " + user + "...");
//		endpoint = userAgent.registerEndPoint(user, TestConfig.DOMAIN,
//				TestConfig.PASS, expires, registerController);
//		event = registerController.pollSipEndPointEvent(TestConfig.WAIT_TIME);
//		assertNotNull(event);
//		assertEquals(EndPointEvent.REGISTER_USER_SUCESSFUL, event.getEventType());
//		log.info("OK");
//
//		log.info("Wait for register keep alive user " + user + "...");
//		event = registerController.pollSipEndPointEvent(expires);
//		assertNotNull(event);
//		assertEquals(EndPointEvent.REGISTER_USER_SUCESSFUL, event.getEventType());
//		log.info("OK");
//
//		log.info("Deregister user " + user + "...");
//		endpoint.terminate();
//		event = registerController.pollSipEndPointEvent(TestConfig.WAIT_TIME);
//		assertNotNull(event);
//		assertEquals(EndPointEvent.REGISTER_USER_SUCESSFUL,event.getEventType());
//		log.info("OK");
//
//		tEnd = System.currentTimeMillis();
//		boolean lessThan = (tEnd - tStart) < (expires * 1000);
//		assertEquals(true, lessThan);
//
//		log.info(" -------------------- Test Register KeepAlive finished OK --------------------");
//	}

}
