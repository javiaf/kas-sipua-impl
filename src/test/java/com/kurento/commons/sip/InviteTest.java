/*
Kurento Sip User Agent implementation.
Copyright (C) <2011>  <Tikal Technologies>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License version 3
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>
*/
package com.kurento.commons.sip;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.event.SipCallEvent;
import com.kurento.commons.sip.event.SipEndPointEvent;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commons.util.Configuration;
import com.kurento.commons.util.MediaSessionDummy;
import com.kurento.commons.util.SipCallController;
import com.kurento.commons.util.SipEndPointController;

public class InviteTest extends TestCase {
	
	private final static Log log = LogFactory.getLog(InviteTest.class);
	
	private Configuration testConfig = Configuration.getInstance();

	SipConfig config;	
	UA userAgent1 ;
	UA userAgent2 ;
	
	@Override
	protected void setUp() throws Exception {
		UaFactory.setMediaSession(new MediaSessionDummy());
		
		config = new SipConfig();
		config.setProxyAddress(Configuration.PROXY_IP);
		config.setProxyPort(Configuration.PROXY_PORT);
		config.setLocalAddress(Configuration.LOCAL_IP);
		int port =Configuration.LOCAL_PORT+testConfig.getCounter();
		config.setLocalPort(port);
		userAgent1 = UaFactory.getInstance(config);
		
		
		SipConfig config2 = new SipConfig();
		config2.setProxyAddress(Configuration.PROXY_IP);
		config2.setProxyPort(Configuration.PROXY_PORT);
		config2.setLocalAddress(Configuration.LOCAL_IP);
		int port2 =Configuration.LOCAL_PORT+testConfig.getCounter();
		config2.setLocalPort(port2);
		userAgent2 = UaFactory.getInstance(config2);

	}
	
	@Override
	protected void tearDown() throws Exception {
		userAgent1.terminate();
		userAgent2.terminate();
	}
	
	
public void testSetupAndDropFromCalling() throws Exception {
		
		log.info("-----------------------------Test for test setup and drop from calling party---------------------------------");
		log.info("User agent initialize with config<< "+ config.toString()+">>");
		SipEndPointController registerAController =  new SipEndPointController("Resgister listener");
		
		String user40Name = Configuration.USER+testConfig.getCounter();
		SipEndPoint endpoint40 = userAgent1.registerEndPoint(user40Name,Configuration.DOMAIN , Configuration.PASS, 3600, registerAController);
		SipEndPointEvent event40 = registerAController.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event40.getEventType());
		
		SipEndPointController register30Controller =  new SipEndPointController("Resgister listener");
		String user30 = Configuration.USER+testConfig.getCounter();
		String user30toCall = "sip:"+user30+"@"+Configuration.DOMAIN;
		SipEndPoint endpoint30 = userAgent2.registerEndPoint(user30, Configuration.DOMAIN, Configuration.PASS, 3600, register30Controller);
		SipEndPointEvent event30 = register30Controller.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event30.getEventType());
		
		SipCallController callListener40 = new SipCallController();
		SipCall initialCall40 = endpoint40.dial(user30toCall, callListener40);
		
		SipEndPointEvent incomingCall30Event = register30Controller.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipEndPointEvent.INCOMING_CALL, incomingCall30Event.getEventType());
		SipCall sipcall30 = incomingCall30Event.getCallSource();
		SipCallController call30Listener = new SipCallController();
		sipcall30.addListener(call30Listener);
		
		sipcall30.accept();
		SipCallEvent call40SetupEvent = callListener40.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipCallEvent.CALL_SETUP, call40SetupEvent.getEventType());
		
		initialCall40.hangup();
		
		SipCallEvent call30TerminateEvent = call30Listener.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipCallEvent.CALL_SETUP,call30TerminateEvent.getEventType());
		
		
		 call40SetupEvent = callListener40.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(call40SetupEvent.getEventType(), SipCallEvent.CALL_TERMINATE);
		
		call30TerminateEvent = call30Listener.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipCallEvent.CALL_TERMINATE,call30TerminateEvent.getEventType());
		
		log.info("-------------------------------Test finished-----------------------------------------");

	}

	public void testSetupAndDropFromOtherParty() throws Exception {
		
		log.info("-----------------------------Test for test setup and drop from calling party---------------------------------");
		log.info("User agent initialize with config<< "+ config.toString()+">>");
		SipEndPointController registerAController =  new SipEndPointController("Resgister listener");
		
		String user40Name = Configuration.USER+testConfig.getCounter();

		SipEndPoint endpoint40 = userAgent1.registerEndPoint(user40Name, Configuration.DOMAIN, Configuration.PASS, 3600, registerAController);
		SipEndPointEvent event40 = registerAController.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event40.getEventType());
		
		SipEndPointController register30Controller =  new SipEndPointController("Resgister listener");
		
		
		String user30 = Configuration.USER+testConfig.getCounter();
		String user30toCall = "sip:"+user30+"@"+Configuration.DOMAIN;
		
		SipEndPoint endpoint30 = userAgent2.registerEndPoint(user30, Configuration.DOMAIN, Configuration.PASS, 3600, register30Controller);
		SipEndPointEvent event30 = register30Controller.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event30.getEventType());
		
		SipCallController callListener40 = new SipCallController();
		SipCall initialCall40 = endpoint40.dial(user30toCall, callListener40);
		
		SipEndPointEvent incomingCall30Event = register30Controller.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipEndPointEvent.INCOMING_CALL, incomingCall30Event.getEventType());
		SipCall sipcall30 = incomingCall30Event.getCallSource();
		SipCallController call30Listener = new SipCallController();
		sipcall30.addListener(call30Listener);
		
		sipcall30.accept();
		//initialCall40.cancel();
		SipCallEvent call40SetupEvent = callListener40.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipCallEvent.CALL_SETUP, call40SetupEvent.getEventType());
		
		SipCallEvent call30TerminateEvent = call30Listener.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipCallEvent.CALL_SETUP,call30TerminateEvent.getEventType());		
		sipcall30.hangup();

		 call40SetupEvent = callListener40.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(call40SetupEvent.getEventType(), SipCallEvent.CALL_TERMINATE);
		
		call30TerminateEvent = call30Listener.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipCallEvent.CALL_TERMINATE,call30TerminateEvent.getEventType());
		
		log.info("-------------------------------Test finished-----------------------------------------");
	
	}

public void testEmptyInvite() throws Exception {
		
		log.info("-----------------------------Test for test setup and drop from calling party---------------------------------");
		log.info("User agent initialize with config<< "+ config.toString()+">>");
		SipEndPointController registerAController =  new SipEndPointController("Resgister listener");
		
		String user40Name = Configuration.USER;
		SipEndPoint endpoint40 = userAgent1.registerEndPoint(user40Name,Configuration.DOMAIN , Configuration.PASS, 3600, registerAController);
		SipEndPointEvent event40 = registerAController.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event40.getEventType());
		
//		SipEndPointController register30Controller =  new SipEndPointController("Resgister listener");
//		String user30 = Configuration.USER+testConfig.getCounter();
//		String user30toCall = "sip:"+user30+"@"+Configuration.DOMAIN;
//		SipEndPoint endpoint30 = userAgent2.registerEndPoint(user30, Configuration.DOMAIN, Configuration.PASS, 3600, register30Controller);
//		SipEndPointEvent event30 = register30Controller.pollSipEndPointEvent(Configuration.WAIT_TIME);
//		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event30.getEventType());
		
//		SipCallController callListener40 = new SipCallController();
//		SipCall initialCall40 = endpoint40.dial(user30toCall, callListener40);
		
		SipEndPointEvent incomingCall30Event = registerAController.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipEndPointEvent.INCOMING_CALL, incomingCall30Event.getEventType());
		SipCall sipcall30 = incomingCall30Event.getCallSource();
		SipCallController call30Listener = new SipCallController();
		sipcall30.addListener(call30Listener);
		
		sipcall30.accept();
//		SipCallEvent call40SetupEvent = callListener40.pollSipEndPointEvent(Configuration.WAIT_TIME);
//		assertEquals(SipCallEvent.CALL_SETUP, call40SetupEvent.getEventType());
		
//		initialCall40.hangup();
		Thread.sleep(1000000);
		
		SipCallEvent call30TerminateEvent = call30Listener.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipCallEvent.CALL_SETUP,call30TerminateEvent.getEventType());
		
//		
//		 call40SetupEvent = callListener40.pollSipEndPointEvent(Configuration.WAIT_TIME);
//		assertEquals(call40SetupEvent.getEventType(), SipCallEvent.CALL_TERMINATE);
		
		call30TerminateEvent = call30Listener.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipCallEvent.CALL_TERMINATE,call30TerminateEvent.getEventType());
		
		log.info("-------------------------------Test finished-----------------------------------------");

	}

}
