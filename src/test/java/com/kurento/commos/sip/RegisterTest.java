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

import com.kurento.commons.sip.SipEndPoint;
import com.kurento.commons.sip.UA;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.event.SipEndPointEvent;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commos.utils.SipEndPointController;
import com.kurento.commos.utils.Configuration;

public class RegisterTest extends TestCase {
	
	private final static Log log = LogFactory.getLog(RegisterTest.class);
	
	private Configuration testConfig = Configuration.getInstance();
	UA userAgent ;
	SipConfig config;
	@Override
	protected void setUp() throws Exception {

	}
	
	@Override
	protected void tearDown() throws Exception {
		if (userAgent != null)
		userAgent.terminate();
	}
	
	
	/*
	 * Test only available with no localhost ip.
	 */
	public void testCRegister() throws Exception {
		
//		log.info("-----------------------------Test for register fail---------------------------------");
//		config = new SipConfig();
//		config.setProxyAddress(TestConfig.LINPHONE_HOST);
//		config.setProxyPort(TestConfig.PROXY_PORT);
//		config.setLocalAddress("193.147.51.20");
//		int port =TestConfig.LOCAL_PORT+testConfig.getCounter();
//		config.setLocalPort(port);
//		config.setPublicAddress("193.147.51.20");
//		config.setPublicPort(port);
//		userAgent = UaFactory.getInstance(config);
//		
//		log.info("User agent initialize with config<< "+ config.toString()+">>");
//		SipEndPointController registerController =  new SipEndPointController("Resgister listener");
//		String user = TestConfig.USER+testConfig.getCounter();
//		SipEndPoint endpoint = userAgent.registerEndPoint(user, TestConfig.LINPHONE_HOST, "none", 300, registerController);
//		SipEndPointEvent event = registerController.pollSipEndPointEvent(TestConfig.WAIT_TIME);
//		assertEquals(SipEndPointEvent.REGISTER_USER_FAIL, event.getEventType());
//		log.info("Register user fail, OK");
//		log.info("-------------------------------Test finished-----------------------------------------");
		
	}
	
	public void testRegisterSuccesfull() throws Exception {
		
		log.info("-----------------------------Test for register Successfull---------------------------------");
		config = new SipConfig();
		config.setProxyAddress(Configuration.PROXY_IP);
		config.setProxyPort(Configuration.PROXY_PORT);
		config.setLocalAddress(Configuration.LOCAL_IP);
		int port =Configuration.LOCAL_PORT+testConfig.getCounter();
		config.setLocalPort(port);
		config.setPublicAddress(Configuration.LOCAL_IP);
		config.setPublicPort(port);
		userAgent = UaFactory.getInstance(config);
		
		
		log.info("User agent initialize with config<< "+ config.toString()+">>");
		SipEndPointController registerController =  new SipEndPointController("Resgister listener");
		String userA = Configuration.USER+testConfig.getCounter();
		SipEndPoint endpoint = userAgent.registerEndPoint(userA,Configuration.DOMAIN, Configuration.PASS, 3000, registerController);
		SipEndPointEvent event = registerController.pollSipEndPointEvent(Configuration.WAIT_TIME);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event.getEventType());
		endpoint.terminate();
		event = registerController.pollSipEndPointEvent(Configuration.WAIT_TIME);
		String userB = Configuration.USER+testConfig.getCounter();
		endpoint = userAgent.registerEndPoint(userB, Configuration.DOMAIN, Configuration.PASS, 3000, registerController);
		assertEquals(SipEndPointEvent.REGISTER_USER_SUCESSFUL, event.getEventType());
		log.info("Register user Succesful, OK");
		log.info("-------------------------------Test finished-----------------------------------------");
		
	}


}
