package com.kurento.commos.utils;

public class Configuration {
	
	private static Configuration instance;
	
	
	public final static String LINPHONE_HOST = "sip.linphone.org";
	public final static String DOMAIN = "kurento.com";
	public final static String USER = "user-test";
	public final static String PASS = "password";
	public final static int  PROXY_PORT = 5060;
	public final static String LOCAL_IP= "127.0.0.1";
	public final static String PROXY_IP= "127.0.0.1";
	public final static int LOCAL_PORT= 5070;
	public final static int WAIT_TIME = 100;
	private static int counter;
		
	
	private  Configuration() {
		org.apache.log4j.BasicConfigurator.configure();
		//counter = 0;
	}
	public static Configuration getInstance() {
		if (instance == null){
			instance = new Configuration();
		}
			
		return instance;
	}
	
	public int getCounter() {
		return counter++;
	}

}
