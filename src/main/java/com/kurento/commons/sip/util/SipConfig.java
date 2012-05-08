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
package com.kurento.commons.sip.util;

/**
 * The SipConfig class is the mechanism used by KAS-SIPUA to easily provide SIP
 * configuration when creating a new IP User Agent. Required configuration
 * includes
 * <ul>
 * <li><b>Local Address</b>: Specifies the network interface where the SIP stack
 * will be binded
 * <li><b>Local Port</b>: Specifies the port number of the Local Address where
 * the SIP stack is listening for new messages
 * <li><b>Proxy Address</b>: Specifies a remote address where the SIP REGISTRAR
 * and SIP PROXY listen for messages
 * <li><b>Proxy Port</b>: Specifies port where the REGISTRAR and PROXY servers
 * listen for messages
 * <li><b>Transport</b>: use UDP or TCP, depending on transport protocol used
 * for SIP
 * <li><b>Max Forwards</b>: Maximun number of hops allowed for SIP messages
 * reaching the User Agent
 * </ul>
 * 
 * @author Kurento
 * 
 */
public class SipConfig {
	private int localPort = 5070;
	private String localAddress = "";

	private String stunServerAddress;
	private int stunServerPort;
	private String proxyAddress = "127.0.0.1";
	private int proxyPort = 5060;
	private String transport = "UDP";
	private int maxForards = 70;

	private boolean enableKeepAlive;

	private long keepAlivePeriod = 5000;

	/**
	 * Get the IP address or interface name where the SIP stack must be
	 * attached. If this value is provided and no matching network interface is
	 * found, the SIP stack will fail to initialize
	 * 
	 * @return localAddress
	 */
	public String getLocalAddress() {
		return localAddress;
	}

	/**
	 * Set the IP address or interface name pattern where the SIP stack will be
	 * attached. If this value is set and no matching interface or IP is found,
	 * the SIP stack will fail to initialize. If this value is not set the SIP
	 * stack will attach to the first non local active interface found.
	 * 
	 * @param localAddress
	 */
	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}

	/**
	 * Returns the local port where the SIP stack binds to
	 * 
	 * @return
	 */
	public int getLocalPort() {
		return localPort;
	}

	/**
	 * Sets the local port where the SIP stack will bind to
	 * 
	 * @param localPort
	 */
	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	/**
	 * Returns the IP address where SIP messages will be sent.
	 * 
	 * @return
	 */
	public String getProxyAddress() {
		return proxyAddress;
	}

	/**
	 * Sets the IP address where SIP messages will be send
	 * 
	 * @param proxyAddress
	 */
	public void setProxyAddress(String proxyAddress) {
		this.proxyAddress = proxyAddress;
	}

	/**
	 * Returns the port of the ProxyAddress where SIP messages will be sent
	 * 
	 * @return Port number where Proxy SIP stack will bind
	 */
	public int getProxyPort() {
		return proxyPort;
	}

	/**
	 * Sets the port where the SIP proxy will be listening for SIP messages
	 * 
	 * @param proxyPort
	 */
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	/**
	 * Gets the transport protocol used to relay SIP messages
	 * 
	 * @return UDP/TCP
	 */
	public String getTransport() {
		return transport;
	}

	/**
	 * Gets the transport protocol used to relay SIP messages
	 * 
	 * @param transport
	 */
	public void setTransport(String transport) {
		this.transport = transport;
	}

	/**
	 * Get the maximum number of hops for SIP messages allowed by the User Agent
	 * 
	 * @return
	 */
	public int getMaxForards() {
		return maxForards;
	}

	/**
	 * Sets the maximum number of hops for SIP messages allowed by the User
	 * Agent
	 * 
	 * @param maxForards
	 */
	public void setMaxForards(int maxForards) {
		this.maxForards = maxForards;
	}

	/**
	 * Get the STUN server IP address
	 * 
	 * @return
	 */
	public String getStunServerAddress() {
		return stunServerAddress;
	}

	/**
	 * Sets the STUN server IP address or hostname
	 * 
	 * @param stunServerAddress
	 */
	public void setStunServerAddress(String stunServerAddress) {
		this.stunServerAddress = stunServerAddress;
	}

	/**
	 * Get the STUN server port where request must be sent
	 * 
	 * @return
	 */
	public int getStunServerPort() {
		return stunServerPort;
	}

	/**
	 * Sets the STUN server port
	 * 
	 * @param stunServerPort
	 */
	public void setStunServerPort(int stunServerPort) {
		this.stunServerPort = stunServerPort;
	}

	/**
	 * Find out if SIP keep-alive packets are requested to be sent
	 * 
	 * @return
	 */
	public boolean isEnableKeepAlive() {
		return enableKeepAlive;
	}

	/**
	 * Set this parameter to true in order to request SIP keep-alive packages.
	 * These are useful in NAT environments, where punch hole is required
	 * 
	 * @param enableKeepAlive
	 */
	public void setEnableKeepAlive(boolean enableKeepAlive) {
		this.enableKeepAlive = enableKeepAlive;
	}

	/**
	 * Get SIP keep-alive interval. Is the period between two consecutive SIP
	 * keep-alive packets when keep-alive is enabled
	 * 
	 * @return
	 */
	public long getKeepAlivePeriod() {
		return keepAlivePeriod;
	}

	/**
	 * Configure SIP keep-alive interval. Is the period between two consecutive SIP
	 * keep-alive packets when keep-alive is enabled
	 * 
	 * @return
	 */
	public void setKeepAlivePeriod(long keepAlivePeriod) {
		this.keepAlivePeriod = keepAlivePeriod;
	}

	public String toString() {
		return "\n" + "Local Port   : " + localPort + "\n" + "Proxy Address: "
				+ proxyAddress + "\n" + "Proxy Port   : " + proxyPort + "\n"
				+ "Transport    : " + transport + "\n" + "Max Forwards : "
				+ maxForards + "\n";
	}

}
