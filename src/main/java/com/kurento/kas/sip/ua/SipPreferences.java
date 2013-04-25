package com.kurento.kas.sip.ua;

import javax.sip.ListeningPoint;

import android.content.Context;
import android.content.SharedPreferences;

public class SipPreferences {

	public static final String PREFERENCES_NAME = "SipPreferences";

	public static final String ONLY_IPV4 = "onlyIpv4";
	public static final String TRANSPORT = "transport";

	public static final String PROXY_SERVER_ADDRESS = "proxyServerAddress"; // Mandatory
	public static final String PROXY_SERVER_PORT = "proxyServerPort"; // Mandatory
	public static final String LOCAL_PORT = "localPort";

	public static final String REG_EXPIRES = "regExpires";

	private boolean onlyIpv4 = true;
	private String transport = ListeningPoint.UDP;

	private String proxyServerAddress;
	private int proxyServerPort;
	private int localPort = 6060;

	private int regExpires = 3600;

	SipPreferences(Context context) throws KurentoSipException {

		SharedPreferences pref = context.getSharedPreferences(PREFERENCES_NAME,
				Context.MODE_PRIVATE);

		proxyServerAddress = pref.getString(PROXY_SERVER_ADDRESS, null);
		if (proxyServerAddress == null)
			throw new KurentoSipException(
					"PROXY_SERVER_ADDRESS not assigned. It is mandatory.");

		proxyServerPort = pref.getInt(PROXY_SERVER_PORT, -1);
		if (proxyServerPort < 1024)
			throw new KurentoSipException(
					"PROXY_SERVER_PORT must be >= 1024. It is mandatory.");

		regExpires = pref.getInt(REG_EXPIRES, regExpires);
		if (regExpires < 0)
			throw new KurentoSipException("REG_EXPIRES must be > 0");

		// TODO: complete preferences
	}

	public boolean isOnlyIpv4() {
		return onlyIpv4;
	}

	public String getTransport() {
		return transport;
	}

	public String getProxyServerAddress() {
		return proxyServerAddress;
	}

	public int getProxyServerPort() {
		return proxyServerPort;
	}

	public int getLocalPort() {
		return localPort;
	}

	public int getRegExpires() {
		return regExpires;
	}

}
