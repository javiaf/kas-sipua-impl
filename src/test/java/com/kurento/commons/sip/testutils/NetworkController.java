package com.kurento.commons.sip.testutils;

import com.kurento.commons.sip.agent.NetworkListener;
import com.kurento.ua.commons.ServerInternalErrorException;

public class NetworkController {
	
	private NetworkListener networkListener;
	
	public void setNetworkListener(NetworkListener listener){
		this.networkListener= listener;
	}
	
	public void execNetworkChange() throws ServerInternalErrorException {
		if (networkListener != null){
			networkListener.networkReconfigure();
		}
	}

}
