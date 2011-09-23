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

public class InviteTest extends TestCase {
	
	private final static Log log = LogFactory.getLog(InviteTest.class);
	
	
	private final static String LINPHONE_HOST = "sip.linphone.org";
	private final static String LINPHONE_USER = "user-test";
	private final static String LINPHONE_PASS = "password";
	private final static int LINPHONE_PORT = 5060;
	private final static String LOCAL_IP= "193.147.51.20";
	private final static String PROXYL_IP= "193.147.51.17";
	private final static String DOMAIN= "tikal.com";
	
	private final int  SERVLET_PORT = 5080; 
	private final static int WAIT_TIME = 100;
	private final static int localPort= 5040;
	SipConfig config;
	UA userAgent ;

	public InviteTest(){
		org.apache.log4j.BasicConfigurator.configure();
	}
	
	@Override
	protected void setUp() throws Exception {
		config = new SipConfig();
		config.setProxyAddress(PROXYL_IP);
		config.setProxyPort(LINPHONE_PORT);
		config.setLocalAddress(LOCAL_IP);
		config.setLocalPort(localPort);
		UaFactory.setMediaSession(new MediaSessionDummy());
		userAgent = UaFactory.getInstance(config);
      

//		UaFactory.setMediaSession(MediaSession)
	}
	
	@Override
	protected void tearDown() throws Exception {
		userAgent.terminate();
	}
	
	
	
	public void testInvite() throws Exception {
		
		log.info("-----------------------------Test for invite to no found call---------------------------------");
		log.info("User agent initialize with config<< "+ config.toString()+">>");
		SipEndPointController registerAController =  new SipEndPointController("Resgister listener");
		SipEndPoint endpointA = userAgent.registerEndPoint(LINPHONE_USER, DOMAIN, LINPHONE_PASS, 3600, registerAController);
		SipEndPointEvent eventA = registerAController.pollSipEndPointEvent(WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, eventA.getEventType());
		
		SipEndPointController registerBController =  new SipEndPointController("Resgister listener");
		String userB = "sip:userB@"+DOMAIN+":"+LINPHONE_PORT;
		SipEndPoint endpointB = userAgent.registerEndPoint("userB", DOMAIN, LINPHONE_PASS, 3600, registerBController);
		SipEndPointEvent eventB = registerBController.pollSipEndPointEvent(WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, eventB.getEventType());
		
		SipCallController callListener = new SipCallController();
		SipCall call = endpointA.dial(userB, callListener);
		//SipCallEvent callToUserNotFoundEvent = callListener.pollSipEndPointEvent(WAIT_TIME);
		//assertEquals(SipCallEvent.CALL_ERROR, callToUserNotFoundEvent.getEventType());
		
		SipEndPointEvent incomingCallEvent = registerBController.pollSipEndPointEvent(WAIT_TIME);
		assertEquals(SipEndPointEvent.INCOMING_CALL, incomingCallEvent.getEventType());
		SipCall sipcall = incomingCallEvent.getCallSource();
		SipCallController callBListener = new SipCallController();
		sipcall.addListener(callBListener);
		sipcall.accept();
		
		
		
		
		SipCallEvent callSetup = callListener.pollSipEndPointEvent(WAIT_TIME);
		assertEquals(SipCallEvent.CALL_SETUP, callSetup.getEventType());
		SipCall sipcallA = callSetup.getSource();

		SipCallEvent callBSetupEvent = callBListener.pollSipEndPointEvent(WAIT_TIME);
		assertEquals(callBSetupEvent.getEventType(), SipCallEvent.CALL_SETUP);
		try {
			sipcall.reject();
			assertTrue(false);
		} catch (ServerInternalErrorException e){
			assertTrue(true);
		}
		log.info("-------------------------------Test finished-----------------------------------------");
		
	}






}
