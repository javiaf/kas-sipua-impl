package com.kurento.commons.sip.junit.api;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.agent.UaImpl;
import com.kurento.commons.sip.testutils.MediaSessionDummy;
import com.kurento.commons.sip.testutils.NetworkController;
import com.kurento.commons.sip.testutils.SipEndPointController;
import com.kurento.commons.sip.testutils.TestConfig;
import com.kurento.commons.sip.testutils.TestTimer;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.ua.commons.EndPoint;
import com.kurento.ua.commons.KurentoUaTimer;
import com.kurento.ua.commons.ServerInternalErrorException;
import com.kurento.ua.commons.UA;
import com.kurento.ua.commons.junit.DialTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({ DialTest.class })
public class ApiDialTest {

	private static final int EXPIRES = 5;

	private static String serverName = "server";
	private static String clientName = "client";

	@BeforeClass
	public static void setupRegisterAndUnregisterTest() throws Exception {
		System.out.println("setupRegisterAndUnregisterTest");

		EndPoint clientEndPoint = createClientEP();
		EndPoint serverEndPoint = createServerEP();
		DialTest.setClientEndPoint(clientEndPoint);
		DialTest.setServerEndPoint(serverEndPoint);
	}

	private static EndPoint createClientEP()
			throws ServerInternalErrorException {
		String localAddress = "lo";
		if (System.getProperty("os.name").startsWith("Mac"))
			localAddress = "lo0";

		UaFactory.setMediaSession(new MediaSessionDummy());
		KurentoUaTimer timer = new TestTimer();

		SipConfig cConfig = new SipConfig();
		cConfig.setProxyAddress(TestConfig.PROXY_IP);
		cConfig.setProxyPort(TestConfig.PROXY_PORT);
		cConfig.setLocalPort(TestConfig.CLIENT_PORT);
		cConfig.setLocalAddress(localAddress);
		cConfig.setTimer(timer);

		UA clientUa = UaFactory.getInstance(cConfig);
		((UaImpl) clientUa).setTestMode(true);

		NetworkController clientNc = new NetworkController();
		clientNc.setNetworkListener(UaFactory.getNetworkListener(clientUa));
		clientNc.execNetworkChange();

		SipEndPointController clientEndPointController = new SipEndPointController(
				"");
		Map<String, Object> cEpConfig = new HashMap<String, Object>();
		cEpConfig.put("SIP_EXPIRES", EXPIRES);
		cEpConfig.put("SIP_RECEIVE_CALL", false);
		EndPoint clientEndPoint = clientUa.registerEndpoint(clientName,
				"kurento.com", clientEndPointController, cEpConfig);

		return clientEndPoint;
	}

	private static EndPoint createServerEP()
			throws ServerInternalErrorException {
		String localAddress = "lo";
		if (System.getProperty("os.name").startsWith("Mac"))
			localAddress = "lo0";

		UaFactory.setMediaSession(new MediaSessionDummy());
		KurentoUaTimer timer = new TestTimer();

		SipConfig sConfig = new SipConfig();
		sConfig.setProxyAddress(TestConfig.CLIENT_IP);
		sConfig.setProxyPort(TestConfig.CLIENT_PORT);
		sConfig.setLocalPort(TestConfig.PROXY_PORT);
		sConfig.setLocalAddress(localAddress);
		sConfig.setTimer(timer);

		UA serverUa = UaFactory.getInstance(sConfig);

		NetworkController serverNc = new NetworkController();
		serverNc.setNetworkListener(UaFactory.getNetworkListener(serverUa));
		serverNc.execNetworkChange();

		SipEndPointController serverEndPointController = new SipEndPointController(
				"");
		Map<String, Object> sEpConfig = new HashMap<String, Object>();
		sEpConfig.put("SIP_RECEIVE_CALL", false);
		EndPoint serverEndPoint = serverUa.registerEndpoint(serverName,
				"kurento.com", serverEndPointController, sEpConfig);

		return serverEndPoint;
	}

}