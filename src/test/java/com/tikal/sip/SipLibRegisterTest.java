package com.tikal.sip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tikal.sip.agent.UaFactory;
import com.tikal.sip.event.SipEndPointEvent;
import com.tikal.sip.event.SipEventType;
import com.tikal.sip.testutil.SipEndPointController;
import com.tikal.sip.util.SipConfig;

public class SipLibRegisterTest {
	private static final Log log = LogFactory.getLog(SipLibRegisterTest.class);
	private static SipConfig config;

	@BeforeClass
	public static void setUp() {
		try {
			PatternLayout layout = new PatternLayout("%d - %-5p - %m%n");
			Logger logger = Logger.getRootLogger();
			logger.addAppender(new ConsoleAppender(layout));
			logger.setLevel(Level.TRACE);
			
			config = new SipConfig();
			// IP address
			config.setLocalAddress("127.0.0.1");
			config.setLocalPort(6060);
			config.setProxyAddress("127.0.0.1");
			config.setProxyPort(5060);


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
	 * <li>Instantiate User and User Controller
	 * <li>Request REGISTER to the SIP User Agent
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
	public void register() throws Exception {
		log.info("============ TEST START ===========");
		log.debug("Start test: register");
		// 1.- Instantiate User Agent
		log.debug("CONFIGURATION: " + config.toString());
		UA ua = UaFactory.getInstance(config);

		// 2.1 - Instantiate Controller
		log.debug("Initialize UA for test register");
		SipEndPointController controller = new SipEndPointController();
	
		// 2.2 - Instantiate User => Register initiated automatically
		log.debug("Initialize User Controllers for test register");
		SipEndPoint endPoint = ua.registerEndPoint("test_registrar", "tikal.com", 3600, controller);
		
		// 3.-   TEST VERIFICATION: Wait event REGISTER SUCCESSFUL
		SipEventType eventType = controller.pollSipEndPointEvent(5).getEventType();
		log.debug("Received SIP Transaction event: "+ eventType);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL,eventType);
		
		// 5.-   UNREGISTER
		endPoint.terminate();
		
		// 6.-  TEST VERIFICATION: Wait event REGISTER SUCCESSFUL
		eventType = controller.pollSipEndPointEvent(5).getEventType();
		log.debug("Received SIP Transaction event: "+ eventType);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL,eventType);
		
		// 7.- TEST VERIFICATION: No contact registered in the registrar   
		ua.terminate();
		log.debug("============= TEST COMPLETE: register:");
	}
	

	/**
	 * <b>REGISTER TIMER</b>
	 * <p>
	 * Test the Stack is able to send a new register when expires time is over
	 * <p>
	 * <b>Procedure</b>
	 * <ol>
	 * <li>Instantiate SIP User Agent
	 * <li>Instantiate User and User Controller
	 * <li>Request REGISTER to the SIP User Agent
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
	public void registerExpiration () throws Exception {
		log.info("============ TEST START ===========");
		log.debug("Start test: register expiration");
		// 1.- Instantiate User Agent
		UA ua = UaFactory.getInstance(config);

		// 2.1 - Instantiate Controller
		log.debug("Initialize UA for test register expiration");
		SipEndPointController controller = new SipEndPointController();
		
		// 2.2 - Instantiate User
		log.debug("Initialize User Controllers for test register");
		SipEndPoint endPoint = ua.registerEndPoint("test_registrar","tikal.com", 10, controller);
		
		// 3.-   TEST VERIFICATION: Wait 1st event REGISTER SUCCESSFUL before 5 seconds
		SipEventType eventType = controller.pollSipEndPointEventType(5);
		log.debug("Received SIP Transaction event: "+ eventType);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL,eventType);

		// 4.-   TEST VERIFICATION: Wait 2nd event REGISTER SUCCESSFUL before 15 seconds
		eventType = controller.pollSipEndPointEventType(15);
		log.debug("Received SIP Transaction event: "+ eventType);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL,eventType);

		// 5.-   UNREGISTER
		endPoint.terminate();
		
		// 7.-  TEST VERIFICATION: Wait event REGISTER SUCCESSFUL
		eventType = controller.pollSipEndPointEventType(5);
		log.debug("Received SIP Transaction event: "+ eventType);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL,eventType);

		// 8.-  TEST VERIFICATION: Expect timeout after unregister takes place
		eventType = controller.pollSipEndPointEventType(15);
		log.debug("Received SIP Transaction event: "+ eventType);
		assertNull(eventType);
		
		ua.terminate();
		log.debug("============= TEST COMPLETE: registerExpiration:");
	}
	
	@AfterClass
	public void tearDown () {
		
	}

}
