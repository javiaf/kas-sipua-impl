package com.kurento.commos.utils;

import com.kurento.commons.mscontrol.Configuration;
import com.kurento.commons.mscontrol.MediaSession;
import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.Parameters;
import com.kurento.commons.mscontrol.mediacomponent.MediaComponent;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;

public class MediaSessionDummy implements MediaSession {

	@Override
	public void release() {
		// TODO Auto-generated method stub

	}

	@Override
	public MediaComponent createMediaComponent(
			Configuration<MediaComponent> arg0, Parameters arg1)
			throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NetworkConnection createNetworkConnection()
			throws MsControlException {
		return new NetworkConnectionDummy();
	}

}
