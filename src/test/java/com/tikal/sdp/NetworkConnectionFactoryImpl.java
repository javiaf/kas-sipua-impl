package com.tikal.sdp;

import javax.media.mscontrol.networkconnection.NetworkConnection;
import javaxt.sdp.SdpFactory;
import javaxt.sdp.SessionDescription;

import com.tikal.media.NetworkConnectionFactory;
import com.tikal.sip.exception.ServerInternalErrorException;

public class NetworkConnectionFactoryImpl implements NetworkConnectionFactory {

	@Override
	public NetworkConnection getInstance() throws ServerInternalErrorException {
		SessionDescription sdp;
		try {
			sdp = SdpFactory.getInstance().createSessionDescription();
			return new NetworkConnectionImpl();
		} catch (Exception e) {
			throw new ServerInternalErrorException(
					"SDP parse error creating NetworkConnection", e);
		}
	}

}
