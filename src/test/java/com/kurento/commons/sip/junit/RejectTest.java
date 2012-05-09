package com.kurento.commons.sip.junit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.sip.agent.EndPointFactory;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.testutils.MediaSessionDummy;
import com.kurento.commons.sip.testutils.SipEndPointController;
import com.kurento.commons.sip.testutils.TestConfig;
import com.kurento.commons.sip.testutils.TestTimer;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.UA;

public class RejectTest {

	private final static Logger log = LoggerFactory.getLogger(RejectTest.class);

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

		log.info("	ServerUa created");

		SipConfig sConfig = new SipConfig();
		sConfig.setProxyAddress(TestConfig.CLIENT_IP);
		sConfig.setProxyPort(TestConfig.CLIENT_PORT);
		sConfig.setLocalPort(TestConfig.PROXY_PORT);
		sConfig.setLocalAddress("lo0");

		clientUa = UaFactory.getInstance(sConfig);
		clientEndPointController = new SipEndPointController("client");
		clientTimer = new TestTimer();
		clientEndPoint = EndPointFactory.getInstance(clientName, "kurento.com",
				"", expires, clientUa, clientEndPointController, clientTimer,
				false);
		// Create SIP stack and activate SIP EndPoints
		clientUa.reconfigure();

		log.info("	ClientUa created");

	}

	@AfterClass
	public static void tearDown() {
		serverUa.terminate();
		clientUa.terminate();
	}

}
