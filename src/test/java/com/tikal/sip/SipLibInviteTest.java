package com.tikal.sip;

import static org.junit.Assert.assertEquals;

import javax.media.mscontrol.join.Joinable.Direction;
import javaxt.sip.address.Address;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.BeforeClass;
import org.junit.Test;
import com.tikal.sdp.NetworkConnectionFactoryImpl;
import com.tikal.sip.agent.UaFactory;
import com.tikal.sip.event.SipCallEvent;
import com.tikal.sip.event.SipEndPointEvent;
import com.tikal.sip.event.SipEventType;
import com.tikal.sip.testutil.SipCallController;
import com.tikal.sip.testutil.SipEndPointController;
import com.tikal.sip.util.SipConfig;

public class SipLibInviteTest {

	private static final Log log = LogFactory.getLog(SipLibInviteTest.class);

	@BeforeClass
	public static void setUp() {
		try {
			PatternLayout layout = new PatternLayout("%d - %-5p - %c{1} - %m%n");
			Logger logger = Logger.getRootLogger();
			logger.addAppender(new ConsoleAppender(layout));
			logger.setLevel(Level.TRACE);
			
			UaFactory.setNetworkConnectionFactory(new NetworkConnectionFactoryImpl());

		} catch (Exception e) {
			log.error("UaFactory initialization error", e);
		}
	}
	
	/**
	 * <b>REGISTER NEW CONTACT</b>
	 * <p>
	 * Test the Stack is able to register a contact within a SIP REGISTRAR
	 * <p>
	 * <b>Procedure</b>
	 * <ol>
	 * <li>Instantiate SIP User Agent
	 * <li>Instantiate User1, User2 and User Controllers
	 * <li>Request REGISTER to the SIP User Agent for both users
	 * <li>Wait for event SipTransactionEvent.REGISTER_USER_SUCESSFUL
	 * <li>Request UNREGISTER to the SIP User Agent
	 * <li>Wait for the event SipTransactionEvent.REGISTER_USER_SUCESSFUL
	 * </ol>
	 * <b> Pass criteria</b>
	 * <ul>
	 * <li> Receive event SipTransactionEvent.REGISTER_USER_SUCESSFUL
	 * </ul>
	 * @throws Exception 
	 */
	@Test
	public void invite() throws Exception {
		log.info("============ TEST START ===========");
		log.debug("Start test: Invite");
		
		// 1.- Instantiate User Agents
		SipConfig configA = new SipConfig();
		// IP address
		configA = new SipConfig();
		// IP address
		configA.setLocalAddress("127.0.0.1");
		configA.setLocalPort(6060);
		configA.setProxyAddress("127.0.0.1");
		configA.setProxyPort(5060);
		
		log.debug("CONFIGURATION User A: " + configA);
		UA uaA = UaFactory.getInstance(configA);
		
		SipConfig configB = new SipConfig();
		// IP address
		configB = new SipConfig();
		// IP address
		configB.setLocalAddress("127.0.0.1");
		configB.setLocalPort(6070);
		configB.setProxyAddress("127.0.0.1");
		configB.setProxyPort(5060);
		
		log.debug("CONFIGURATION User B: " + configB);
		UA uaB = UaFactory.getInstance(configB);

		// 2.1 - Instantiate Controller
		log.debug("Initialize UA for test register");
		SipEndPointController controllerA = new SipEndPointController();
		SipEndPointController controllerB = new SipEndPointController();
		
		// 2.2 - Instantiate User
		log.debug("Initialize User Controllers for test register");
		SipEndPoint endPointA = uaA.registerEndPoint("usera", "tikal.com", 3600, controllerA);
		SipEndPoint endPointB = uaB.registerEndPoint("userb", "tikal.com", 3600, controllerB);
		
		// 3.-   TEST VERIFICATION: Wait event REGISTER SUCCESSFUL
		SipEventType eventTypeA = controllerA.pollSipEndPointEventType(5);
		log.debug("Received SIP Transaction event: "+ eventTypeA);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL,eventTypeA);

		SipEventType eventTypeB = controllerB.pollSipEndPointEventType(5);
		log.debug("Received SIP Transaction event: "+ eventTypeB);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL,eventTypeB);
		
		// 4.-   INVITE
		
		// 4.1-  Dial from A
		log.debug("UserA dials UserB");
		Address userB = UaFactory.getAddressFactory().createAddress("sip:userb@tikal.com");
		SipCallController callControllerA = new SipCallController();
		log.debug("userB.toString(): " + userB.toString());
		SipCall callAtA = endPointA.dial(userB.toString(), Direction.DUPLEX, callControllerA);
		
		// 4.2-  Wait incoming call at B
		SipEndPointEvent eventB = controllerB.pollSipEndPointEvent(5);
		log.debug("User A sent Invite. User B received event: " + eventB.getEventType());
		assertEquals(SipEndPointEvent.INCOMING_CALL,eventB.getEventType());
		
		// 4.3-  ACCEPT CALL at B
		SipCall callAtB = eventB.getCallSource();
		SipCallController callControllerB = new SipCallController();
		callAtB.addListener(callControllerB);
		log.debug("User B accepts call");
		callAtB.accept();
		log.debug("\n\nUser B accept OK\n");
		
		// 4.4- Wait confirm at A
		eventTypeA = callControllerA.pollSipEndPointEventType(50000);
		log.debug("User B sent response. User A received event: " + eventTypeA);
		assertEquals(SipCallEvent.CALL_SETUP, eventTypeA);
		
		// 4.5- Wait confirm at B
		eventTypeB = callControllerB.pollSipEndPointEventType(500000);
		log.debug("User B sent response. User A received event: " + eventTypeB);
		assertEquals(SipCallEvent.CALL_SETUP, eventTypeB);
		
		// 5- End call
		log.debug("Hang up the call from side B");
		callAtB.hangup();
		
		// 5.1- Wait event at A side
		eventTypeA = callControllerA.pollSipEndPointEventType(5);
		log.debug("User B sent hanged the call. User A received event: " + eventTypeA);
		assertEquals(SipCallEvent.CALL_TERMINATE, eventTypeA);

		// 5.2- Wait event at B side
		eventTypeB = callControllerB.pollSipEndPointEventType(5);
		log.debug("User B sent hanged the call. User B received event: " + eventTypeA);
		assertEquals(SipCallEvent.CALL_TERMINATE, eventTypeB);

		log.debug("Removing Users...");
		endPointA.terminate();
		endPointB.terminate();
		
		uaA.terminate();
		uaB.terminate();
				
		log.debug("============= TEST COMPLETE: invite:");	}
}
