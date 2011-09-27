package com.kurento.commos.sip;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kurento.commons.sip.SipCall;
import com.kurento.commons.sip.SipEndPoint;
import com.kurento.commons.sip.UA;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.event.SipCallEvent;
import com.kurento.commons.sip.event.SipEndPointEvent;
import com.kurento.commons.sip.exception.ServerInternalErrorException;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commos.utils.MediaSessionDummy;
import com.kurento.commos.utils.SipCallController;
import com.kurento.commos.utils.SipEndPointController;

public class RejectTest extends TestCase {
	
	private final static Log log = LogFactory.getLog(RejectTest.class);
	
	
//	private final static String LINPHONE_HOST = "sip.linphone.org";
	private final static String LINPHONE_USER = "user-test";
	private final static String LINPHONE_PASS = "password";
	private final static int LINPHONE_PORT = 5060;
	private final static String LOCAL_IP= "193.147.51.20";
	private final static String PROXYL_IP= "193.147.51.28";
//	private final static String DOMAIN= "tikal.com";
	private final static String DOMAIN= "sip.linphone.org";
	
	private final int  SERVLET_PORT = 5080; 
	private final static int WAIT_TIME = 100;
	private final static int localPort= 5040;
	SipConfig config;	
	private static boolean initalized;
	UA userAgent1 ;
	UA userAgent2 ;

	public RejectTest(){
		if (!initalized) {
			initalized = true;
			org.apache.log4j.BasicConfigurator.configure();
		}
	}
	
	@Override
	protected void setUp() throws Exception {
		config = new SipConfig();
		config.setProxyAddress(PROXYL_IP);
		config.setProxyPort(LINPHONE_PORT);
		config.setLocalAddress(LOCAL_IP);
		config.setLocalPort(localPort);
		UaFactory.setMediaSession(new MediaSessionDummy());
		userAgent1 = UaFactory.getInstance(config);
		
		SipConfig config2 = new SipConfig();
		config2.setProxyAddress(PROXYL_IP);
		config2.setProxyPort(LINPHONE_PORT);
		config2.setLocalAddress(LOCAL_IP);
		config2.setLocalPort(5030);
		userAgent2 = UaFactory.getInstance(config2);
      

//		UaFactory.setMediaSession(MediaSession)
	}
	
	@Override
	protected void tearDown() throws Exception {
		userAgent1.terminate();
		userAgent2.terminate();
	}
	
	public void testReject() throws Exception {
		
		log.info("-----------------------------Test for reject call---------------------------------");
		log.info("User agent initialize with config<< "+ config.toString()+">>");
		SipEndPointController registerAController =  new SipEndPointController("Resgister listener");
		
		String user40Name = "user40";
		SipEndPoint endpoint40 = userAgent1.registerEndPoint(LINPHONE_USER, PROXYL_IP, LINPHONE_PASS, 3600, registerAController);
		SipEndPointEvent event40 = registerAController.pollSipEndPointEvent(WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event40.getEventType());
		
		SipEndPointController register30Controller =  new SipEndPointController("Resgister listener");
		String user30 = "sip:quizh@"+PROXYL_IP+":"+LINPHONE_PORT;
		SipEndPoint endpoint30 = userAgent2.registerEndPoint("quizh", PROXYL_IP, "linphone123", 3600, register30Controller);
		SipEndPointEvent event30 = register30Controller.pollSipEndPointEvent(WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event30.getEventType());
		
		SipCallController callListener40 = new SipCallController();
		SipCall initialCall40 = endpoint40.dial(user30, callListener40);
		
		SipEndPointEvent incomingCall30Event = register30Controller.pollSipEndPointEvent(WAIT_TIME+20000);
		assertEquals(SipEndPointEvent.INCOMING_CALL, incomingCall30Event.getEventType());
		SipCall sipcall30 = incomingCall30Event.getCallSource();
		SipCallController call30Listener = new SipCallController();
		sipcall30.addListener(call30Listener);

		sipcall30.reject();
				
		
		SipCallEvent callSetup40 = callListener40.pollSipEndPointEvent(WAIT_TIME);
		assertEquals(SipCallEvent.CALL_REJECT, callSetup40.getEventType());
	
		SipCallEvent call30SetupEvent = call30Listener.pollSipEndPointEvent(WAIT_TIME);
	
		
		log.info("-------------------------------Test finished-----------------------------------------");
	
	}


}
