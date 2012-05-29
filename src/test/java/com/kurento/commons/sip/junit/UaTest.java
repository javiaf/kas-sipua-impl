package com.kurento.commons.sip.junit;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.testutils.MediaSessionDummy;
import com.kurento.commons.sip.testutils.SipEndPointController;
import com.kurento.commons.sip.testutils.TestConfig;
import com.kurento.commons.sip.testutils.TestErrorException;
import com.kurento.commons.sip.testutils.TestTimer;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.UA;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public class UaTest {
	
	private final static Logger log = LoggerFactory.getLogger(RegisterTest.class);
	
	private static TestTimer timer;

	private static String domain = "kurento.com";
	private static String name = "ua";
	private static String uri = "sip:" + name + "@" + domain;
	private static int expires = 6;
	private static String localAddress;

	private static EndPoint endPoint;
	
	/**
	 * Verify the UA does not allow any SIP message to be send if the SIP stack finds any initialization problem
	 * <pre>
	 * C:Bad UA init         :S Rise exception and disable UA
	 * C:---SIP MSG----X	 :S 
	 * </pre>
	 * 
	 * Associated to case: #256
	 * 
	 * @throws Exception 
	 */
	@Test(expected = TestErrorException.class)
	public void testBadUaInitDueToPort() throws TestErrorException {
		
		log.info("-------------------- Test Bad UA initialization due to PORT --------------------");
		
		if (System.getProperty("os.name").startsWith("Mac"))
			localAddress = "lo0";
		else
			localAddress = "lo";

		log.info("Initialice SIP UA Cancel test on platform: "
				+ System.getProperty("os.name"));
		
		UaFactory.setMediaSession(new MediaSessionDummy());

		// Provide wrong configuration (port 0) to avoid correct initialization
		timer = new TestTimer();
		SipConfig config = new SipConfig();
		config.setProxyAddress(TestConfig.CLIENT_IP);
		config.setProxyPort(TestConfig.CLIENT_PORT);
		config.setLocalPort(0);
		config.setLocalAddress(localAddress);
		config.setTimer(timer);

		
		// Get UA instance
		UA ua = UaFactory.getInstance(config);
		
		// Initialize SIP stack
		try {
			ua.reconfigure();
		} catch (ServerInternalErrorException e) {
			log.info("UA throwed exception during initialization", e);
			throw new TestErrorException("UA throwed exception during initialization",e);
		}
		
	}

	/**
	 * Verify the UA does not allow any SIP message to be send if the SIP stack finds any initialization problem
	 * <pre>
	 * C:Bad UA init         :S Rise exception and disable UA
	 * C:---SIP MSG----X	 :S 
	 * </pre>
	 * 
	 * Associated to case: #256
	 * 
	 * @throws Exception 
	 */
	@Test(expected = TestErrorException.class)
	public void testBadUaInitDueToInterface() throws TestErrorException {
		
		log.info("-------------------- Test Bad UA initialization due to interface --------------------");
		
		localAddress = "non-exists";

		UaFactory.setMediaSession(new MediaSessionDummy());

		// Provide wrong configuration (port 0) to avoid correct initialization
		timer = new TestTimer();
		SipConfig config = new SipConfig();
		config.setProxyAddress(TestConfig.CLIENT_IP);
		config.setProxyPort(TestConfig.CLIENT_PORT);
		config.setLocalPort(TestConfig.PROXY_PORT);
		config.setLocalAddress(localAddress);
		config.setTimer(timer);

		
		// Get UA instance
		UA ua = UaFactory.getInstance(config);
		
		// Initialize SIP stack
		try {
			ua.reconfigure();
		} catch (ServerInternalErrorException e) {
			log.info("UA throwed exception during initialization", e);
			throw new TestErrorException("UA throwed exception during initialization",e);
		}
	}
	
	/**
	 * Verify the UA does not allow any SIP message to be send if the SIP stack finds any initialization problem
	 * <pre>
	 * C:Bad UA init         :S Rise exception and disable UA
	 * C:---SIP MSG----X	 :S 
	 * </pre>
	 * 
	 * Associated to case: #256
	 * 
	 * @throws Exception 
	 */
	@Test(expected = TestErrorException.class)
	public void testBadUaInitDueToAlreadyBind() throws TestErrorException {
		
		log.info("-------------------- Test Bad UA initialization due to interface --------------------");
		
		if (System.getProperty("os.name").startsWith("Mac"))
			localAddress = "lo0";
		else
			localAddress = "lo";

		UaFactory.setMediaSession(new MediaSessionDummy());

		// Provide wrong configuration (port 0) to avoid correct initialization
		timer = new TestTimer();
		SipConfig config = new SipConfig();
		config.setProxyAddress(TestConfig.CLIENT_IP);
		config.setProxyPort(TestConfig.CLIENT_PORT);
		config.setLocalPort(TestConfig.PROXY_PORT);
		config.setLocalAddress(localAddress);
		config.setTimer(timer);

		
		// Get UA instances
		UA ua1 = UaFactory.getInstance(config);
		UA ua2 = UaFactory.getInstance(config);
		
		// Initialize SIP stack
		try {
			ua1.reconfigure();
		} catch (ServerInternalErrorException e) {
			log.info("UA throwed exception during initialization", e);
			assertTrue("Misplaced exception",false);
		}
		try {
			ua2.reconfigure();
		} catch (ServerInternalErrorException e) {
			log.info("UA throwed exception during initialization", e);
			throw new TestErrorException("UA throwed exception during initialization",e);
		}
	}
}
