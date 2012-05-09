package com.kurento.commons.sip.junit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.sip.testutils.SipEndPointController;
import com.kurento.commons.sip.testutils.TestTimer;
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.UA;

public class RejectTest {

	private final static Logger log = LoggerFactory.getLogger(RejectTest.class);

	private static UA serverUa;
	private static UA clientUa;

	private static SipEndPointController serverEndPointController;
	private static SipEndPointController clientEndPointController;

	private static TestTimer serverTimer;
	private static TestTimer clientTimer;

	private static String domain = "kurento.com";
	private static String serverName = "server";
	private static String clientName = "client";
	private static String serverUri = "sip:" + serverName + "@" + domain;
	private static String clientUri = "sip:" + clientName + "@" + domain;

	private static EndPoint serverEndPoint;
	private static EndPoint clientEndPoint;

}
