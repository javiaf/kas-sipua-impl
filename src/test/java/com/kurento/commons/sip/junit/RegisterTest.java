package com.kurento.commons.sip.junit;

import static org.junit.Assert.assertTrue;

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
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.UA;
import com.kurento.commons.ua.event.EndPointEvent;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public class RegisterTest {

	private final static Logger log = LoggerFactory
			.getLogger(RegisterTest.class);

	private static UA serverUa;
	private static UA clientUa;

	private static SipEndPointController serverEndPointController;
	private static SipEndPointController clientEndPointController;

	private static TestTimer serverTimer;
	private static TestTimer clientTimer;

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
		serverUa.reconfigure();
		serverEndPointController = new SipEndPointController("server");
		serverTimer = new TestTimer();
		serverEndPoint = EndPointFactory.getInstance("server", "kurento.com",
				"", 10, serverUa, serverEndPointController, serverTimer);

		SipConfig sConfig = new SipConfig();
		sConfig.setProxyAddress(TestConfig.CLIENT_IP);
		sConfig.setProxyPort(TestConfig.CLIENT_PORT);
		sConfig.setLocalPort(TestConfig.PROXY_PORT);
		sConfig.setLocalAddress("lo0");

		clientUa = UaFactory.getInstance(sConfig);
		clientUa.reconfigure();
		clientEndPointController = new SipEndPointController("client");
		serverTimer = new TestTimer();
		clientEndPoint = EndPointFactory.getInstance("clien", "kurento.com",
				"", 10, clientUa, clientEndPointController, clientTimer);
	}

	@Test
	public void test() throws ServerInternalErrorException, InterruptedException {
		SipCallController clientCallController = new SipCallController("client");
		clientEndPoint.dial("sip:server@kurento.com:" + TestConfig.PROXY_PORT, clientCallController);
		EndPointEvent event = serverEndPointController.pollSipEndPointEvent(1);
		assertTrue("Missing incoming call on server side", event==null);
		log.debug("Found event: " + event.toString());
	}

}
