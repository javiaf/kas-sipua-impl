package com.kurento.commons.sip.junit.integration;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;

import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.testutils.MediaSessionDummy;
import com.kurento.commons.sip.testutils.NetworkController;
import com.kurento.commons.sip.testutils.TestConfig;
import com.kurento.commons.sip.testutils.TestTimer;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.ua.commons.KurentoUaTimer;
import com.kurento.ua.commons.ServerInternalErrorException;
import com.kurento.ua.commons.UA;

public abstract class KcUaRegisterTestBase {

	protected static final int EXPIRES = 5;

	protected static UA createClientUA() throws ServerInternalErrorException {
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

		UA clientUA = UaFactory.getInstance(cConfig);

		NetworkController clientNc = new NetworkController();
		clientNc.setNetworkListener(UaFactory.getNetworkListener(clientUA));
		clientNc.execNetworkChange();

		return clientUA;
	}

	protected static Map<String, Object> createServerEpConfig() {
		Map<String, Object> sEpConfig = new HashMap<String, Object>();
		sEpConfig.put("SIP_RECEIVE_CALL", false);

		return sEpConfig;
	}

	protected static UA createServerUA() throws ServerInternalErrorException {
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

		UA serverUA = UaFactory.getInstance(sConfig);

		NetworkController serverNc = new NetworkController();
		serverNc.setNetworkListener(UaFactory.getNetworkListener(serverUA));
		serverNc.execNetworkChange();

		return serverUA;
	}

	protected static Map<String, Object> createClientEpConfig() {
		Map<String, Object> cEpConfig = new HashMap<String, Object>();
		cEpConfig.put("SIP_EXPIRES", EXPIRES);
		cEpConfig.put("SIP_RECEIVE_CALL", true);

		return cEpConfig;
	}

	@AfterClass
	public static void thearDownSuite() {
		System.out.println("Tests down");
	}

}