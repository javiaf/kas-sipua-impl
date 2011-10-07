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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kurento.commons.sip.UA;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commos.utils.Configuration;
import com.kurento.commos.utils.MediaSessionDummy;

import de.javawi.jstun.attribute.MessageAttributeException;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.test.DiscoveryInfo;
import de.javawi.jstun.test.DiscoveryTest;
import de.javawi.jstun.util.UtilityException;

public class KeepAliveTest extends TestCase {

	private final static Log log = LogFactory.getLog(KeepAliveTest.class);

	private Configuration testConfig = Configuration.getInstance();
	SipConfig config;
	UA userAgent1;
	UA userAgent2;

	@Override
	protected void setUp() throws Exception {
		UaFactory.setMediaSession(new MediaSessionDummy());

		int port = Configuration.LOCAL_PORT + testConfig.getCounter();
		
		config = stun("192.168.1.104",port );
		String info= "Local : "+config.getLocalAddress()+":"+config.getLocalPort()+
		"\n public : "+config.getPublicAddress()+":"+config.getPublicPort();
		log.info(info);
		userAgent1 = UaFactory.getInstance(config);

	}

	@Override
	protected void tearDown() throws Exception {
		userAgent1.terminate();
		userAgent2.terminate();
	}

	private SipConfig stun(String localAddress, int localPort) {
		SipConfig config = new SipConfig();
		
		
		config.setProxyAddress(Configuration.PROXY_IP);
		config.setProxyPort(Configuration.PROXY_PORT);
		config.setLocalAddress(localAddress);
		config.setLocalPort(localPort);
		
		
		try {
			InetAddress addr = InetAddress.getByName(localAddress);
			DiscoveryTest test = new DiscoveryTest(addr, localPort,
					"stun.sipgate.net", 10000);

			DiscoveryInfo info = test.test();

			InetAddress publicAddress = info.getPublicIP();
			int publicPort = info.getPublicPort();
			config.setPublicAddress(publicAddress.getHostAddress());
			config.setPublicPort(publicPort);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessageAttributeParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessageHeaderParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UtilityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessageAttributeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return config;
	}

	public void testCancel() throws Exception {
		userAgent1.registerEndPoint("hola", "urjc.es","hola", 1000,null);
		Thread.sleep(1000000);

	}

}
