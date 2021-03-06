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


import com.kurento.commons.sip.testutils.TestConfig.SdpPortManagerType;
import com.kurento.mscontrol.commons.MsControlException;
import com.kurento.mscontrol.commons.join.Joinable;
import com.kurento.mscontrol.commons.join.JoinableStream;
import com.kurento.mscontrol.commons.join.JoinableStream.StreamType;
import com.kurento.mscontrol.commons.networkconnection.NetworkConnection;
import com.kurento.mscontrol.commons.networkconnection.SdpPortManager;

public class NetworkConnectionDummy implements NetworkConnection {
	SdpPortManagerDummy sdpManager;
	

	NetworkConnectionDummy() {
		sdpManager = new SdpPortManagerDummy();
		
	}
	
	public void setSdpType(SdpPortManagerType sdpType) {
		sdpManager.setSdpType(sdpType);
	}

	public void setSdpGenerateTimer (int sdpGenerateTimer){
		sdpManager.setSdpGenerateTimer(sdpGenerateTimer);
	}
	
	public void setSdpProcessTimer (int sdpProcessTimer) {
		sdpManager.setSdpProcessTimer(sdpProcessTimer);
	}

	@Override
	public JoinableStream getJoinableStream(StreamType arg0)
			throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JoinableStream[] getJoinableStreams() throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Joinable[] getJoinees() throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Joinable[] getJoinees(Direction arg0) throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void join(Direction arg0, Joinable arg1) throws MsControlException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unjoin(Joinable arg0) throws MsControlException {
		// TODO Auto-generated method stub

	}

	@Override
	public void confirm() throws MsControlException {
		// TODO Auto-generated method stub

	}

	@Override
	public void release() {
		// TODO Auto-generated method stub

	}

	@Override
	public SdpPortManager getSdpPortManager() throws MsControlException {
		return sdpManager;
	}

	@Override
	public long getBitrate(StreamType streamType, Direction direction) {
		// TODO Auto-generated method stub
		return 0;
	}

}
