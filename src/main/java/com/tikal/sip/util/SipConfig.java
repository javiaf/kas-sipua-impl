package com.tikal.sip.util;

public class SipConfig {
	private String localAddress = "127.0.0.1";
	private int localPort = 5070;
	private String proxyAddress = "127.0.0.1";
	private int proxyPort = 5060;
	private String transport = "UDP";
	private int maxForards = 70;
	
	public SipConfig() {
		
	}
	
	public String getLocalAddress() {
		return localAddress;
	}
	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}
	public int getLocalPort() {
		return localPort;
	}
	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}
	public String getProxyAddress() {
		return proxyAddress;
	}
	public void setProxyAddress(String proxyAddress) {
		this.proxyAddress = proxyAddress;
	}
	public int getProxyPort() {
		return proxyPort;
	}
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}
	public String getTransport() {
		return transport;
	}
	public void setTransport(String transport) {
		this.transport = transport;
	}
	public int getMaxForards() {
		return maxForards;
	}
	public void setMaxForards(int maxForards) {
		this.maxForards = maxForards;
	}

	public String toString() {
		return "\n"
			+"Local Address: " + localAddress +"\n"
			+"Local Port   : " + localPort+"\n"
			+"Proxy Address: " + proxyAddress+"\n"
			+"Proxy Port   : " + proxyPort+"\n"
			+"Transport    : " + transport+"\n"
			+"Max Forwards : " + maxForards+"\n"; 
	}
	
}
