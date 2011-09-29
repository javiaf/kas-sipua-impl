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
package com.kurento.commos.sip;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kurento.commons.mscontrol.join.Joinable.Direction;
import com.kurento.commons.sip.SipEndPoint;
import com.kurento.commons.sip.SipEndPointListener;
import com.kurento.commons.sip.UA;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.event.SipEndPointEvent;
import com.kurento.commons.sip.event.SipEventType;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commos.utils.SipEndPointController;

public class RegisterTest extends TestCase {
	
	private final static Log log = LogFactory.getLog(RegisterTest.class);
	
	
	private final static String LINPHONE_HOST = "sip.linphone.org";
	private final static String LINPHONE_USER = "user-test";
	private final static String LINPHONE_PASS = "password";
	private final static int LINPHONE_PORT = 5060;
	private final static String LOCAL_IP= "193.147.51.20";
	
	private final static int WAIT_TIME = 100;
	private final static int localPort= 5040;
	SipConfig config;
	UA userAgent ;

	public RegisterTest(){
		org.apache.log4j.BasicConfigurator.configure();
	}
	
	@Override
	protected void setUp() throws Exception {
		config = new SipConfig();
		config.setProxyAddress(LINPHONE_HOST);
		config.setProxyPort(LINPHONE_PORT);
		config.setLocalAddress(LOCAL_IP);
		config.setLocalPort(localPort);
		userAgent = UaFactory.getInstance(config);
	}
	
	@Override
	protected void tearDown() throws Exception {
		userAgent.terminate();
	}
	
	
	
	public void testCRegister() throws Exception {
		
		log.info("-----------------------------Test for register fail---------------------------------");
		
		
		log.info("User agent initialize with config<< "+ config.toString()+">>");
		SipEndPointController registerController =  new SipEndPointController("Resgister listener");
		SipEndPoint endpoint = userAgent.registerEndPoint(LINPHONE_USER, LINPHONE_HOST, "none", 3000, registerController);
		SipEndPointEvent event = registerController.pollSipEndPointEvent(WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_FAIL, event.getEventType());
		log.info("Register user fail, OK");
		log.info("-------------------------------Test finished-----------------------------------------");
		
	}
	
	public void testRegisterSuccesfull() throws Exception {
		
		log.info("-----------------------------Test for register Successfull---------------------------------");
		log.info("User agent initialize with config<< "+ config.toString()+">>");
		SipEndPointController registerController =  new SipEndPointController("Resgister listener");
		SipEndPoint endpoint = userAgent.registerEndPoint(LINPHONE_USER, LINPHONE_HOST, LINPHONE_PASS, 3000, registerController);
		SipEndPointEvent event = registerController.pollSipEndPointEvent(WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event.getEventType());
		endpoint.terminate();
		event = registerController.pollSipEndPointEvent(WAIT_TIME);
		endpoint = userAgent.registerEndPoint(LINPHONE_USER, LINPHONE_HOST, LINPHONE_PASS, 3000, registerController);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event.getEventType());
		log.info("Register user Succesful, OK");
		log.info("-------------------------------Test finished-----------------------------------------");
		
	}

	public void testRegisterNotAuth() throws Exception {
		
		log.info("-----------------------------Test for register without authetication---------------------------------");
		config = new SipConfig();
		config.setProxyAddress("193.147.51.37");
		config.setProxyPort(5080);
		config.setLocalAddress(LOCAL_IP);
		config.setLocalPort(5060);
		userAgent = UaFactory.getInstance(config);
		log.info("User agent initialize with config<< "+ config.toString()+">>");
		SipEndPointController registerController =  new SipEndPointController("Resgister listener");
		SipEndPoint endpoint = userAgent.registerEndPoint(LINPHONE_USER, LINPHONE_HOST, LINPHONE_PASS, 3000, registerController);
		SipEndPointEvent event = registerController.pollSipEndPointEvent(WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event.getEventType());
		log.info("Register user Succesful, OK");
		log.info("-------------------------------Test finished-----------------------------------------");
		
	}


}
