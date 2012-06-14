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
import com.kurento.commons.sip.testutils.NetworkController;
import com.kurento.commons.sip.testutils.SipEndPointController;
import com.kurento.commons.sip.testutils.TestConfig;
import com.kurento.commons.sip.testutils.TestTimer;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.UA;

public class BugTest {
	private final static Logger log = LoggerFactory
			.getLogger(RegisterTest.class);

	private static UA serverUa;
	private static UA clientUa;
	private static UA secondClientUa;

	private static SipEndPointController serverEndPointController;
	private static SipEndPointController clientEndPointController;
	private static SipEndPointController secondClientEndPointController;

	private static NetworkController serverNc;
	private static NetworkController clientNc;

	private static TestTimer serverTimer;
	private static TestTimer clientTimer;
	private static TestTimer secondClientTimer;;

	private static String domain = "kurento.com";
	private static String serverName = "server";
	private static String clientName = "client";
	private static String secondClientName = "secondClient";
	private static String serverUri = "sip:" + serverName + "@" + domain;
	private static String clientUri = "sip:" + clientName + "@" + domain;
	private static String secondClientUri = "sip:" + secondClientName + "@"
			+ domain;
	private static int expires = 120;
	private static String localAddress;

	private static EndPoint serverEndPoint;
	private static EndPoint clientEndPoint;
	private static EndPoint secondClientEndPoint;

	@BeforeClass
	public static void initTest() throws Exception {

		log.info("Initialice SIP UA Cancel test");

		if (System.getProperty("os.name").startsWith("Mac"))
			localAddress = "lo0";
		else
			localAddress = "lo";

		UaFactory.setMediaSession(new MediaSessionDummy());

		SipConfig sConfig = new SipConfig();
		sConfig.setProxyAddress(TestConfig.PROXY_IP);
		sConfig.setProxyPort(TestConfig.PROXY_PORT);
		sConfig.setLocalPort(TestConfig.CLIENT_PORT);
		sConfig.setLocalAddress(localAddress);
		sConfig.setTimer(serverTimer);

		serverUa = UaFactory.getInstance(sConfig);
		serverEndPointController = new SipEndPointController(serverName);
		serverTimer = new TestTimer();
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

		SipConfig cConfig = new SipConfig();
		cConfig.setProxyAddress(TestConfig.CLIENT_IP);
		cConfig.setProxyPort(TestConfig.CLIENT_PORT);
		cConfig.setLocalPort(TestConfig.PROXY_PORT);
		cConfig.setLocalAddress(localAddress);
		cConfig.setTimer(clientTimer);

		clientUa = UaFactory.getInstance(cConfig);
		clientEndPointController = new SipEndPointController("client");
		clientTimer = new TestTimer();
		Map<String, Object> cEpConfig = new HashMap<String, Object>();
		cEpConfig.put("SIP_RECEIVE_CALL", false);
		clientEndPoint = clientUa.registerEndpoint(clientName, "kurento.com",
				clientEndPointController, cEpConfig);
		// clientEndPoint = EndPointFactory.getInstance(clientName,
		// "kurento.com",
		// "", expires, clientUa, clientEndPointController, false);
		// Create SIP stack and activate SIP EndPoints
		clientNc = new NetworkController();
		clientNc.setNetworkListener(UaFactory.getNetworkListener(clientUa));
		clientNc.execNetworkChange();

	}

	@AfterClass
	public static void tearDown() {
		if (serverUa != null)
			serverUa.terminate();
		if (clientUa != null)
			clientUa.terminate();
	}

	/**
	 * Ticket #42
	 * 
	 * <pre>
	 * C:---INVITE---------->:S
	 * C:<----------200 OK---:S
	 * C:---ACK------------->:S
	 * C1:---INVITE--------->:S
	 * C1:<--BUSY------------:S
	 * C1:----------200 OK-->:S
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReceiveInviteDuringCall() throws Exception {
		// TODO K-Phone must send the message
	}

	/**
	 * Ticket #191
	 * 
	 * C:---INVITE---------->:S<br>
	 * C:<----------INVITE---:S<br>
	 * 
	 * @throws Exception
	 */
	// TODO Define the messages correctly
	@Test
	public void testSendInviteAndReceiveInvite() throws Exception {
		// TODO
	}

	/**
	 * Ticket #217
	 * 
	 * @throws Exception
	 */
	// TODO Define the messages correctly
	@Test
	public void testSendCancelAfterInviteCrash() throws Exception {
		// TODO
	}

	/**
	 * Ticket #235
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSendRegisterAfterExpires() throws Exception {
		// TODO Fixed at RegisterTest
	}

	// FIXED at Cancel Test
	/**
	 * Ticket #187
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSendInviteAndSendCancel() throws Exception {
		// TODO Fixed at CancelTest
	}

}
