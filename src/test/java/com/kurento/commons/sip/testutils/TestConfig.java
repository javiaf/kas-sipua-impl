package com.kurento.commons.sip.testutils;

public class TestConfig {

	public final static String INVALID_DOMAIN = "invaliddomain.com";
	public final static String DOMAIN = "kurento.com";
	public final static String USER = "user-test";
	public final static String PASS = "password";

	public final static String CLIENT_IP= "127.0.0.1";
	public final static int CLIENT_PORT = 5090;

	public final static String PROXY_IP= "127.0.0.1";
	public final static int PROXY_PORT = 5060;

	public final static int WAIT_TIME = 6;
	private static int counter;

	public static int getCounter() {
		return counter++;
	}

}
