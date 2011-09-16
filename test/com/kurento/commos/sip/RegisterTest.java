package com.kurento.commos.sip;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kurento.commons.sip.SipEndPoint;
import com.kurento.commons.sip.SipEndPointListener;
import com.kurento.commons.sip.UA;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.event.SipEndPointEvent;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commos.utils.SipEndPointController;

public class RegisterTest extends TestCase {
	
	private final static Log log = LogFactory.getLog(RegisterTest.class);
	private final static String HOST_37 = "193.147.51.37";
	private final static String HOST2 = "193.147.51.17";
	private final static String LINPHONE_HOST = "sip.linphone.org";
	private final static String LINPHONE_USER = "quizh";
	private final static String LINPHONE_PASS = "linphone123";
	private final static int LINPHONE_PORT = 5060;
	private final static String LOCAL_IP= "193.147.51.20";
	
	private final static int WAIT_TIME = 100;
//	private final static String HOST = LOCAL_IP;
	private final static int proxyPort= 5080;
	private final static int localPort= 5040;
	
//	boolean isRegister;
//	SipEndPoint endpoint;
	
	public RegisterTest(){
		org.apache.log4j.BasicConfigurator.configure();
	}
	
	@Override
	protected void setUp() throws Exception {

	}
	
	@Override
	protected void tearDown() throws Exception {
		
	}
	
	
	
	public void testCRegister() throws Exception {
		
		log.info("--------------------------------------------------------------");
		SipConfig config = new SipConfig();
		config.setProxyAddress(HOST_37);
		config.setProxyPort(proxyPort);
		config.setLocalAddress(LOCAL_IP);
		config.setLocalPort(localPort);
		UA userAgent = UaFactory.getInstance(config);
		log.info("User agent initialize with config<< "+ config.toString()+">>");
		SipEndPointController registerController =  new SipEndPointController("Resgister listener");
		SipEndPoint endpoint = userAgent.registerEndPoint("quizh", "urjc.es", "linphone123", 3000, registerController);
		
		 SipEndPointEvent event = registerController.pollSipEndPointEvent(WAIT_TIME);
		
		 log.info("Event type is "+event.getEventType());

		userAgent.terminate();

		
		//assertEquals(true, isRegister);
		log.info("Test finished");
		
		//call.cancel();
		
	}


}
