package com.kurento.commons.sip.junit.api;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.kurento.ua.commons.UA;
import com.kurento.ua.commons.junit.RegisterTimeOutTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({ RegisterTimeOutTest.class })
public class ApiRegisterTimeOutTest extends ApiRegisterTestBase {

	@BeforeClass
	public static void setupRegisterTimeOutTest() throws Exception {
		System.out.println("setupRegisterTimeOutTest");

		UA clientUA = createClientUA();
		Map<String, Object> cEpConfig = createClientEpConfig();
		RegisterTimeOutTest.setcEpConfig(cEpConfig);
		RegisterTimeOutTest.setClientUA(clientUA);
	}

}