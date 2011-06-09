package com.tikal.sdp;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;

import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.JoinEventListener;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.AllocationEventListener;
import javaxt.sdp.SdpException;

import com.tikal.media.NetworkConnectionBase;
import com.tikal.media.format.SessionSpec;

public class NetworkConnectionImpl extends NetworkConnectionBase {

	private static int port = 10000;
	private SessionSpec template;
	private static final String sdpTemplate = "v=0\r\n"
			+ "o=- 0 0 IN IP4 0.0.0.0\r\n" + "s=-\r\n" + "c=IN IP4 0.0.0.0\r\n"
			+ "t=0 0\r\n" + "m=audio 0 RTP/AVP 106\r\n" + "a=sendrecv\r\n"
			+ "a=rtpmap:106 AMR/8000\r\n" + "a=fmtp:106 octet-align=1\r\n"
			+ "m=video 0 RTP/AVP 96 97\r\n" + "a=rtpmap:96 H263-2000/90000\r\n"
			+ "a=fmtp:96 PROFILE=0;LEVEL=10\r\n" + "a=rtpmap:97 H264/90000";

	protected NetworkConnectionImpl() throws SdpException {
		super();
		template = new SessionSpec(sdpTemplate);
	}

	@Override
	public JoinableStream getJoinableStream(StreamType arg0) {
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
	public void joinInitiate(Direction arg0, Joinable arg1, Serializable arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unjoin(Joinable arg0) throws MsControlException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unjoinInitiate(Joinable arg0, Serializable arg1)
			throws MsControlException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addListener(JoinEventListener arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public MediaSession getMediaSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeListener(JoinEventListener arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void confirm() throws MsControlException {
		// TODO Auto-generated method stub

	}

	@Override
	public MediaConfig getConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> R getResource(Class<R> arg0) throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void triggerAction(Action arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public Parameters createParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<MediaObject> getMediaObjects() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends MediaObject> Iterator<T> getMediaObjects(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Parameters getParameters(Parameter[] arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setParameters(Parameters arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addListener(AllocationEventListener arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeListener(AllocationEventListener arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public SessionSpec generateSessionSpec() {
		return template;
	}

	@Override
	public void setLocalSessionSpec(SessionSpec localSpec) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setRemoteSessionSpec(SessionSpec remote) {
		// TODO Auto-generated method stub
	}

	@Override
	public InetAddress getLocalAddress() {
		InetAddress value = null;
		try {
			value = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
		}
		return value;
	}

}
