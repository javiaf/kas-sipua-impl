package com.kurento.commons.sip.testutils;
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


import com.kurento.commons.mscontrol.Configuration;
import com.kurento.commons.mscontrol.MediaSession;
import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.Parameters;
import com.kurento.commons.mscontrol.mediacomponent.MediaComponent;
import com.kurento.commons.mscontrol.mediamixer.MediaMixer;
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

	@Override
	public NetworkConnection createNetworkConnection(
			Configuration<NetworkConnection> predefinedConfig)
			throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MediaMixer createMediaMixer(
			Configuration<MediaMixer> predefinedConfig, Parameters params)
			throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MediaMixer createMediaMixer() throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

}
