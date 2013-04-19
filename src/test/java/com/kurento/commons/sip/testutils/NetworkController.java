package com.kurento.commons.sip.testutils;

import com.kurento.commons.ua.exception.ServerInternalErrorException;
import com.kurento.kas.sip.ua.NetworkListener;

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
