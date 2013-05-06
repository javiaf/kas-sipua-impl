package com.kurento.kas.sip.ua;

import javax.sip.ListeningPoint;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SipPreferences {

	public static final String ONLY_IPV4 = "ONLY_IPV4";
	public static final String SIP_TRANSPORT = "TRANSPORT";

	public static final String SIP_PROXY_SERVER_ADDRESS = "PROXY_SERVER_ADDRESS"; // Mandatory
	public static final String SIP_PROXY_SERVER_PORT = "PROXY_SERVER_PORT"; // Mandatory
	public static final String SIP_LOCAL_PORT = "LOCAL_PORT";

	public static final String SIP_REG_EXPIRES = "REG_EXPIRES";

	private boolean onlyIpv4 = true;
	private String transport = ListeningPoint.UDP;

	private String proxyServerAddress;
	private int proxyServerPort;
	private int localPort = 6060;

	private int regExpires = 3600;

	SipPreferences(Context context) throws KurentoSipException {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		proxyServerAddress = pref.getString(SIP_PROXY_SERVER_ADDRESS, null);
		if (proxyServerAddress == null)
			throw new KurentoSipException(
					"PROXY_SERVER_ADDRESS not assigned. It is mandatory.");

		proxyServerPort = pref.getInt(SIP_PROXY_SERVER_PORT, -1);
		if (proxyServerPort < 1024)
			throw new KurentoSipException(
					"PROXY_SERVER_PORT must be >= 1024. It is mandatory.");

		regExpires = pref.getInt(SIP_REG_EXPIRES, regExpires);
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
