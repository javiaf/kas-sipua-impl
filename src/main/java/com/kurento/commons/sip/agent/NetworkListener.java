package com.kurento.commons.sip.agent;

import com.kurento.ua.commons.ServerInternalErrorException;

public interface NetworkListener {
	
	public void networkReconfigure() throws ServerInternalErrorException;

}
