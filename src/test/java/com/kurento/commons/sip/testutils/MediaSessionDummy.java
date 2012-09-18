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


import com.kurento.commons.config.Parameters;
import com.kurento.commons.sip.testutils.TestConfig.SdpPortManagerType;
import com.kurento.mscontrol.commons.Configuration;
import com.kurento.mscontrol.commons.MediaSession;
import com.kurento.mscontrol.commons.MsControlException;
import com.kurento.mscontrol.commons.mediacomponent.MediaComponent;
import com.kurento.mscontrol.commons.mediamixer.MediaMixer;
import com.kurento.mscontrol.commons.networkconnection.NetworkConnection;

public class MediaSessionDummy implements MediaSession {
	
	private int sdpGenerateTimer;
	private int sdpProcessTimer;
	private SdpPortManagerType sdpType;

	public void setSdpGenerateTimer (int sdpGenerateTimer) {
		this.sdpGenerateTimer = sdpGenerateTimer;
	}
	
	public void setSdpProcessTimer(int sdpProcessTimer) {
		this.sdpProcessTimer = sdpProcessTimer;
	}
	
	public void setSdpType(SdpPortManagerType sdpType) {
		this.sdpType = sdpType;
	}

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
		
		NetworkConnectionDummy nc = new NetworkConnectionDummy();
		if (sdpGenerateTimer > 0) 
			nc.setSdpGenerateTimer(sdpGenerateTimer);
		if (sdpProcessTimer > 0)
			nc.setSdpProcessTimer(sdpProcessTimer);
		
		nc.setSdpType(sdpType);
		return nc;
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
