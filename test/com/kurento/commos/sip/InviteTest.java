package com.kurento.commos.sip;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kurento.commons.mscontrol.Parameters;
import com.kurento.commons.mscontrol.join.Joinable.Direction;
import com.kurento.commons.sdp.enums.MediaType;
import com.kurento.commons.sdp.enums.Mode;
import com.kurento.commons.sip.SipCall;
import com.kurento.commons.sip.SipEndPoint;
import com.kurento.commons.sip.UA;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.event.SipCallEvent;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commos.utils.SipCallController;
import com.kurento.commos.utils.SipEndPointController;

public class InviteTest extends TestCase {
	
	private final static Log log = LogFactory.getLog(InviteTest.class);
	
	
	private final static String LINPHONE_HOST = "sip.linphone.org";
	private final static String LINPHONE_USER = "user-test";
	private final static String LINPHONE_PASS = "password";
	private final static int LINPHONE_PORT = 5060;
	private final static String LOCAL_IP= "193.147.51.20";
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
		config.setProxyAddress(LOCAL_IP);
		config.setProxyPort(LINPHONE_PORT);
		config.setLocalAddress(LOCAL_IP);
		config.setLocalPort(localPort);
		userAgent = UaFactory.getInstance(config);
//		UaFactory.setMediaSession(MediaSession)
	}
	
	@Override
	protected void tearDown() throws Exception {
		userAgent.terminate();
	}
	
	
	
	public void testCRegister() throws Exception {
		
		log.info("-----------------------------Test for invite to no found call---------------------------------");
		log.info("User agent initialize with config<< "+ config.toString()+">>");
		SipEndPointController registerAController =  new SipEndPointController("Resgister listener");
		SipEndPoint endpointA = userAgent.registerEndPoint(LINPHONE_USER, LOCAL_IP, LINPHONE_PASS, 3600, registerAController);
//		SipEndPointEvent eventA = registerAController.pollSipEndPointEvent(WAIT_TIME);
//		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, eventA.getEventType());
		
		SipEndPointController registerBController =  new SipEndPointController("Resgister listener");
		String userB = "sip:userB@"+DOMAIN+":"+LINPHONE_PORT;
		SipEndPoint endpointB = userAgent.registerEndPoint("userB", DOMAIN, LINPHONE_PASS, 3600, registerBController);
//		SipEndPointEvent eventB = registerBController.pollSipEndPointEvent(WAIT_TIME);
//		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, eventB.getEventType());
		
		SipCallController callListener = new SipCallController();
		SipCall call = endpointA.dial(userB, Direction.DUPLEX, callListener);
		SipCallEvent callToUserNotFoundEvent = callListener.pollSipEndPointEvent(WAIT_TIME);
		assertEquals(SipCallEvent.CALL_ERROR, callToUserNotFoundEvent.getEventType());
		log.info("Call user not found, OK");
		log.info("-------------------------------Test finished-----------------------------------------");
		
	}
//	public void initUA(ArrayList<AudioCodecType> audioCodecs,
//            ArrayList<VideoCodecType> videoCodecs, InetAddress localAddress,
//            NetIF netIF, Map<MediaType, Mode> callDirectionMap, Integer maxBW,
//            Integer maxFR, Integer gopSize, Integer maxQueueSize,
//            String proxyIP, int proxyPort, String localUser, String localPassword, String localRealm)
//            throws Exception {
//
//        Parameters params = new ParametersImpl();
//        params.put(MediaSessionAndroid.NET_IF, netIF);
//        params.put(MediaSessionAndroid.LOCAL_ADDRESS, localAddress);
//        params.put(MediaSessionAndroid.MAX_BANDWIDTH, maxBW);
//
//        params.put(MediaSessionAndroid.STREAMS_MODES, callDirectionMap);
//        params.put(MediaSessionAndroid.AUDIO_CODECS, audioCodecs);
//        params.put(MediaSessionAndroid.VIDEO_CODECS, videoCodecs);
//
//        params.put(MediaSessionAndroid.FRAME_SIZE, null);
//        params.put(MediaSessionAndroid.MAX_FRAME_RATE, maxFR);
//        params.put(MediaSessionAndroid.GOP_SIZE, gopSize);
//        params.put(MediaSessionAndroid.FRAMES_QUEUE_SIZE, maxQueueSize);

//        Log.d(LOG_TAG, "createMediaSession...");
//        mediaSession = MSControlFactory.createMediaSession(params);
//        Log.d(LOG_TAG, "mediaSession: " + this.mediaSession);
//        UaFactory.setMediaSession(mediaSession);
//
//        SipConfig sipConfig = new SipConfig();
//        sipConfig.setLocalAddress(localAddress.getHostAddress());
//        sipConfig.setLocalPort(6060);
//        sipConfig.setProxyAddress(proxyIP);
//        sipConfig.setProxyPort(proxyPort);
//
//        Log.d(LOG_TAG, "CONFIGURATION User Agent: " + sipConfig);
//
//        if (ua != null) {
//            ua.terminate();
//            Log.d(LOG_TAG, "UA Terminate");
//        }
//
//        ua = UaFactory.getInstance(sipConfig);
//
//        register(localUser, localPassword, localRealm);
//    }





}
