package com.kurento.commos.utils;

import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.join.Joinable;
import com.kurento.commons.mscontrol.join.JoinableStream;
import com.kurento.commons.mscontrol.join.JoinableStream.StreamType;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManager;

public class NetworkConnectionDummy implements NetworkConnection {
	SdpPortManager sdpManager;
	NetworkConnectionDummy() {
		sdpManager = new SdpPortManagerDummy();
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

}
