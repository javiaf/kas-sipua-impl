package com.kurento.commons.sip.agent;

import com.kurento.commons.ua.exception.ServerInternalErrorException;

public interface NetworkListener {
	
	public void networkReconfigure() throws ServerInternalErrorException;

}
