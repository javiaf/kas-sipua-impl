package com.tikal.media;

import javax.media.mscontrol.networkconnection.NetworkConnection;

import com.tikal.sip.exception.ServerInternalErrorException;

public interface NetworkConnectionFactory {
	public NetworkConnection getInstance()  throws ServerInternalErrorException;
}
