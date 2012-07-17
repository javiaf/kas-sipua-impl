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

import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.testutils.MediaSessionDummy;
import com.kurento.commons.sip.testutils.NetworkController;
import com.kurento.commons.sip.testutils.SipCallController;
import com.kurento.commons.sip.testutils.SipEndPointController;
import com.kurento.commons.sip.testutils.SipTransactionMonitor;
import com.kurento.commons.sip.testutils.TestConfig;
import com.kurento.commons.sip.testutils.TestTimer;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.ua.commons.Call;
import com.kurento.ua.commons.CallEvent;
import com.kurento.ua.commons.EndPoint;
import com.kurento.ua.commons.EndPointEvent;
import com.kurento.ua.commons.KurentoUaTimer;
import com.kurento.ua.commons.ServerInternalErrorException;
import com.kurento.ua.commons.UA;

/**
 * RFC 3261 Chapter 9. Canceling a Request.
 */
public class CancelTest {

	private final static Logger log = LoggerFactory.getLogger(CancelTest.class);

	private static UA serverUa;
	private static UA clientUa;

	private static SipEndPointController serverEndPointController;
	private static SipEndPointController clientEndPointController;
	
	private static NetworkController serverNc;
	private static NetworkController clientNc;

	private static KurentoUaTimer timer;

	private static String domain = "kurento.com";
	private static String serverName = "server";
	private static String clientName = "client";
	private static String serverUri = "sip:" + serverName + "@" + domain;
	private static String clientUri = "sip:" + clientName + "@" + domain;
	private static int expires = 120;
	private static String localAddress;

	private static EndPoint serverEndPoint;
	private static EndPoint clientEndPoint;

	private static SipTransactionMonitor serverSipMonitor;
	private static SipTransactionMonitor clientSipMonitor;

	@BeforeClass
	public static void initTest() throws Exception {

		if (System.getProperty("os.name").startsWith("Mac"))
			localAddress = "lo0";
		else
			localAddress = "lo";

		log.info("Initialice SIP UA Cancel test on platform: "
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
		serverNc = new NetworkController();
		serverNc.setNetworkListener(UaFactory.getNetworkListener(serverUa));
		serverNc.execNetworkChange();
		// // Listen SIP events
		// serverSipMonitor = new SipTransactionMonitor(serverName);
		// ((UaImpl) serverUa).addUaSipListener(serverSipMonitor);

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
		// "", 10, clientUa, clientEndPointController, false);
		// Create SIP stack and activate SIP EndPoints
		clientNc = new NetworkController();
		clientNc.setNetworkListener(UaFactory.getNetworkListener(clientUa));
		clientNc.execNetworkChange();
		// // Listen SIP events
		// clientSipMonitor = new SipTransactionMonitor(clientName);
		// ((UaImpl) clientUa).addUaSipListener(clientSipMonitor);

	}

	@AfterClass
	public static void tearDown() {
		if (serverUa != null)
			serverUa.terminate();
		if (clientUa != null)
			clientUa.terminate();
	}

	/**
	 * Verify a call is canceled in both sides if CANCEL request arrives before
	 * INVITE response is sent
	 * 
	 * <pre>
	 * C:---INVITE------------------------------->:S
	 * C:<-------------------------- 100 Trying---:S
	 * C:<------------------------- 180 Ringing---:S
	 * C:---CANCEL ------------------------------>:S
	 * C:<------------200 OK (CSeq: xxx CANCEL)---:S
	 * C:<---------------487 Request Terminated---:S
	 * C:----ACK--------------------------------->:S (Call cancel in both sides)
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Test()
	public void testCancel() throws Exception {
		log.info("-------------------- Test Cancel --------------------");

		EndPointEvent endPointEvent;
		CallEvent callEvent;

		// C:-----INVITE-------------->:S
		log.info(clientName + " dial to " + serverName + "...");
		SipCallController callControllerClient = new SipCallController(
				clientName);
		Call clientCall = clientEndPoint.dial(serverUri,
				callControllerClient);
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
		SipCallController callControllerServer = new SipCallController(
				serverName);
		serverCall.addListener(callControllerServer);
		log.info("OK");
		
		// Client  expects CALL_RINGING
		log.info(clientName + " expects ringing from " + serverName
				+ "...");
		callEvent = callControllerClient
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", callEvent != null);
		assertTrue("Bad message received in client UA",
				CallEvent.CALL_RINGING.equals(callEvent.getEventType()));
		log.info("OK");

		// C:----CANCEL -------------->:S
		log.info(clientName + " cancel call...");
		clientCall.terminate();
		log.info("OK");

		log.info(serverName + " expects cancel from " + clientName + "...");
		callEvent = callControllerServer
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		assertTrue("Bad message received in server UA",
				CallEvent.CALL_CANCEL.equals(callEvent.getEventType()));
		log.info("OK");

		// C:<---------------487 Request Terminated---:S
		// C:----ACK--------------------------------->:S

		/*
		 * For non 2xx responses ACK messages are automatically handled by the
		 * SipStack and does not get notified to the SIP listener. No test
		 * verification can be done on this
		 */

		log.info(" -------------------- Test Cancel finished OK --------------------");

	}

//	/**
//	 * 
//	 * Verify call setup is complete if CANCEL request arrives after final
//	 * response is sent
//	 * 
//	 * <pre>
//	 * C:---INVITE----------------------------->:S
//	 * C:<-------------------------100 Trying---:S
//	 * C:<------------------------180 Ringing---:S
//	 * C:<--------- 200 OK (CSeq: xxx INVITE)---:S
//	 * C:---CANCEL----------------------------->:S (Rises exception)
//	 * C:<--------- 200 OK (CSeq: xxx CANCEL)---:S
//	 * C:----ACK ------------------------------>:S (Call setup successfully)
//	 * </pre>
//	 * 
//	 * @throws Exception
//	 */
//	@Test(expected = ServerInternalErrorException.class)
//	public void testCancelAfterAccept() throws Exception {
//		log.info("-------------------- Test Cancel After Accept --------------------");
//
//		EndPointEvent endPointEvent;
//		CallEvent callEvent;
//
//		// C:-----INVITE-------------->:S
//		log.info(clientName + " dial to " + serverName + "...");
//		SipCallController clientCallController = new SipCallController(
//				clientName);
//		Call clientCall = clientEndPoint.dial(serverUri, clientCallController);
//		log.info("OK");
//
//		// Verify server receives event INCOMING_CALL
//		log.info(serverName + " expects incoming call from " + clientName
//				+ "...");
//		endPointEvent = serverEndPointController
//				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
//		assertTrue("No message received in server UA", endPointEvent != null);
//		assertTrue(
//				"Bad message received in server UA: "
//						+ endPointEvent.getEventType(),
//				EndPointEvent.INCOMING_CALL.equals(endPointEvent.getEventType()));
//		
//		// Client  expects CALL_RINGING
//		log.info(clientName + " expects ringing from " + serverName
//				+ "...");
//		callEvent = clientCallController
//				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
//		assertTrue("No message received in client UA", callEvent != null);
//		assertTrue("Bad message received in client UA",
//				CallEvent.CALL_RINGING.equals(callEvent.getEventType()));
//		log.info("OK");
//
//		// C:<--------- 200 OK (CSeq: xxx INVITE)---:S
//		// Accept call
//		Call serverCall = endPointEvent.getCallSource();
//		SipCallController serverCallController = new SipCallController(
//				serverName);
//		serverCall.addListener(serverCallController);
//		log.info("OK");
//
//		log.info(serverName + " accepts call...");
//		serverCall.accept();
//		log.info("OK");
//
//		// C:----ACK ------------------------------>:S (Call setup successfully)
//		// Call set up in both: client and server
//		log.info(clientName + " expects call setup...");
//		callEvent = clientCallController
//				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
//		assertTrue("No message received in server UA", callEvent != null);
//		assertTrue("Bad message received in server UA",
//				CallEvent.CALL_SETUP.equals(callEvent.getEventType()));
//		log.info("OK");
//
//		log.info(serverName + " expects call setup...");
//		callEvent = serverCallController
//				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
//		assertTrue("No message received in server UA", callEvent != null);
//		assertTrue("Bad message received in server UA",
//				CallEvent.CALL_SETUP.equals(callEvent.getEventType()));
//		log.info("OK");
//
//		// C:---CANCEL----------------------------->:S
//		log.info(clientName + " cancel call...");
//		clientCall.hangup();
//		log.info("OK");
//
//		log.info(" -------------------- Test Cancel After Accept finished OK --------------------");
//	}

	/**
	 * Verify the call is setup when CANCEL from client and final response from
	 * server are sent simultaneously
	 * 
	 * <pre>
	 * C:INVITE-------------------------------------------->:S
	 * C:<----------------------------------- 100 Trying ---:S
	 * C:<---------------------------------- 180 Ringing ---:S
	 * C:CANCEL-----> X <------ 200 OK (CSeq: xxx INVITE)---:S
	 * C:ACK ---------------------------------------------->:S
	 * </pre>
	 * 
	 * Associated to case #203
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCancelSimultaneousAccept() throws Exception {
		log.info("-------------------- Test Cancel with simultaneous accept --------------------");

		EndPointEvent endPointEvent;
		CallEvent callEvent;

		// C:-----INVITE-------------->:S
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

		final Call finalServerCall = endPointEvent.getCallSource();
		SipCallController serverCallController = new SipCallController(
				serverName);
		finalServerCall.addListener(serverCallController);
		log.info("OK");
		
		// Client  expects CALL_RINGING
		log.info(clientName + " expects ringing from " + serverName
				+ "...");
		callEvent = clientCallController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in client UA", callEvent != null);
		assertTrue("Bad message received in client UA",
				CallEvent.CALL_RINGING.equals(callEvent.getEventType()));
		log.info("OK");

		// C: <------ 200 OK (CSeq: xxx INVITE)---:S
		log.info(serverName + " accepts call...");
		new Thread(new Runnable() {
			public void run() {
				try {
					finalServerCall.accept();
				} catch (ServerInternalErrorException e) {
					log.error("Unable to accept call", e);
				}
			}
		}).start();

		log.info("OK");

		// C:------CANCEL----->:S
		log.info(clientName + " cancel call...");
		// Wait just a moment to assure the cancel will be issued after response
		Thread.sleep(1);
		clientCall.terminate();
		log.info("OK");

		// C:ACK ---------------------------------------------->:S
		// Client directly expects CALL_TERMINATE
		log.info(clientName + " expects CALL_TERMINATE...");
		callEvent = clientCallController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		assertTrue("Bad message received in server UA",
				CallEvent.CALL_TERMINATE.equals(callEvent.getEventType()));

		log.info("OK");

		// Server will receive CALL_SETUP
		log.info(serverName + " expects CALL_SETUP...");
		callEvent = serverCallController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		assertTrue("Bad message received in server UA",
				CallEvent.CALL_SETUP.equals(callEvent.getEventType()));
		log.info("OK");

		// and immediately receive CALL_TERMIANTE
		log.info(serverName + " expects CALL_TERMINATE...");
		callEvent = serverCallController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		assertTrue("Bad message received in server UA",
				CallEvent.CALL_TERMINATE.equals(callEvent.getEventType()));
		log.info("OK");

		log.info(" -------------------- Test Cancel with simultaneous accept finished OK --------------------");
	}

	/**
	 * Verify the call does not progress in receiving peer when CANCEL is
	 * received while waiting local SDP
	 * 
	 * <pre>
	 * C:INVITE-------------------------------------------->:S
	 * C:<----------------------------------- 100 Trying ---:S
	 * C:<---------------------------------- 180 Ringing ---:S
	 * C:--- CANCEL --------------------------------------->:S
	 * C:<----------------------200 OK (CSeq: xxx CANCEL)---:S
	 * C:<-------------------------487 Request Terminated---:S
	 * C:                                                   :S SDP OK => INCOMING_CALL to Controller
	 * </pre>
	 * 
	 * Associated to case #312
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCancelWhilePeerWaitsLocalSdp() throws Exception {
		log.info("-------------------- Test Cancel while peer waits local SDP --------------------");

		EndPointEvent endPointEvent;
		CallEvent callEvent;

		// Add sleep timer to Media stack
		((MediaSessionDummy) UaFactory.getMediaSession()).setSdpProcessTimer(1000);

		// C:-----INVITE-------------->:S
		log.info(clientName + " dial to " + serverName + "...");
		SipCallController clientCallController = new SipCallController(
				clientName);
		Call clientCall = clientEndPoint.dial(serverUri,
				clientCallController);
		log.info("OK");

		// Client will receive CALL_RINGING event
		log.info(clientName + " expects call ringing  ...");
		callEvent = clientCallController.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		assertTrue("Call terminate expected in client UA",
						CallEvent.CALL_RINGING.equals(callEvent.getEventType()));
		log.info("OK");
		
		// Send inmediate cancel
		// C:----CANCEL -------------->:S
		log.info(clientName + " cancel call...");
		Thread.sleep(1);
		clientCall.terminate();
		log.info("OK");
				
		// Client will receive call cancel event
		log.info(clientName + " expects call cancel  ...");
		callEvent = clientCallController.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		assertTrue("Call terminate expected in client UA",
				CallEvent.CALL_CANCEL.equals(callEvent.getEventType()));
		log.info("OK");
		
		// Client will receive call terminate event
		log.info(clientName + " expects call terminate  ...");
		callEvent = clientCallController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("No message received in server UA", callEvent != null);
		assertTrue("Call terminate expected in client UA",
				CallEvent.CALL_TERMINATE.equals(callEvent.getEventType()));
		log.info("OK");
		

		// Server controller will not receive any event. It will cancel the request silently
		log.info(serverName + " expects no event associated to " + clientName
				+ "...");
		endPointEvent = serverEndPointController
				.pollSipEndPointEvent(TestConfig.WAIT_TIME);
		assertTrue("Message received in server UA", endPointEvent == null);
		log.info("OK");
	}
	
}
