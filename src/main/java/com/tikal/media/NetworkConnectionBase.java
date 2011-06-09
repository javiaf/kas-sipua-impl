package com.tikal.media;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.URI;
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

import com.tikal.media.format.SessionSpec;

public abstract class NetworkConnectionBase implements NetworkConnection {

	private static final long serialVersionUID = 6063974578686207746L;
	
	protected SdpPortManager sdpPortManager;
	
	protected NetworkConnectionBase(){		
		sdpPortManager = new SdpPortManagerImpl(this);
	}
	
	@Override
	public abstract JoinableStream getJoinableStream(StreamType arg0);

	@Override
	public abstract JoinableStream[] getJoinableStreams() throws MsControlException;

	@Override
	public abstract Joinable[] getJoinees() throws MsControlException;

	@Override
	public abstract Joinable[] getJoinees(Direction arg0) throws MsControlException;

	@Override
	public abstract void join(Direction arg0, Joinable arg1) throws MsControlException;

	@Override
	public abstract void joinInitiate(Direction arg0, Joinable arg1, Serializable arg2);

	@Override
	public abstract void unjoin(Joinable arg0) throws MsControlException;

	@Override
	public abstract void unjoinInitiate(Joinable arg0, Serializable arg1)
			throws MsControlException;

	@Override
	public abstract void addListener(JoinEventListener arg0);

	@Override
	public abstract MediaSession getMediaSession();

	@Override
	public abstract void removeListener(JoinEventListener arg0);

	@Override
	public abstract void confirm() throws MsControlException;

	@Override
	public abstract MediaConfig getConfig();

	@Override
	public abstract <R> R getResource(Class<R> arg0) throws MsControlException;

	@Override
	public abstract void triggerAction(Action arg0);

	@Override
	public abstract Parameters createParameters();

	@Override
	public abstract Iterator<MediaObject> getMediaObjects();

	@Override
	public abstract <T extends MediaObject> Iterator<T> getMediaObjects(Class<T> arg0);

	@Override
	public abstract Parameters getParameters(Parameter[] arg0);

	@Override
	public abstract URI getURI();

	@Override
	public abstract void release();

	@Override
	public abstract void setParameters(Parameters arg0);

	@Override
	public abstract void addListener(AllocationEventListener arg0);

	@Override
	public abstract void removeListener(AllocationEventListener arg0);

	@Override
	public SdpPortManager getSdpPortManager() throws MsControlException {
		return sdpPortManager;
	}
	
	/**
	 * Gets a session template copy with all medias capacities and ports assigned.
	 * @return
	 */
	public abstract SessionSpec generateSessionSpec();
	
	/**
	 * Indicates own medias and ports assigned.
	 * Warning: It could has inconsistencies with template.
	 * @param localSpec
	 */	
	public abstract void setLocalSessionSpec(SessionSpec localSpec);
	/**
	 * Indicates other party medias and ports assigned.
	 * @return
	 */
	public abstract void setRemoteSessionSpec(SessionSpec remote);
	public abstract InetAddress getLocalAddress();

}
