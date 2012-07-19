package com.kurento.commons.sip.junit.api;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.kurento.ua.commons.UA;
import com.kurento.ua.commons.junit.RegisterAndUnregisterTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({ RegisterAndUnregisterTest.class })
public class ApiRegisterAndUnregisterTest extends ApiRegisterTestBase {

	@BeforeClass
	public static void setupRegisterAndUnregisterTest() throws Exception {
		System.out.println("setupRegisterAndUnregisterTest");

		UA serverUA = createServerUA();
		Map<String, Object> sEpConfig = createServerEpConfig();
		RegisterAndUnregisterTest.setsEpConfig(sEpConfig);
		RegisterAndUnregisterTest.setServerUA(serverUA);

		UA clientUA = createClientUA();
		Map<String, Object> cEpConfig = createClientEpConfig();
		RegisterAndUnregisterTest.setcEpConfig(cEpConfig);
		RegisterAndUnregisterTest.setClientUA(clientUA);
	}

}