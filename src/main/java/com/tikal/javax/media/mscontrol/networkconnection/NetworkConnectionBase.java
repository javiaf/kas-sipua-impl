package com.tikal.javax.media.mscontrol.networkconnection;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;

import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.AllocationEventListener;

import com.tikal.javax.media.mscontrol.join.JoinableContainerImpl;
import com.tikal.media.format.SessionSpec;


public abstract class NetworkConnectionBase extends JoinableContainerImpl implements
		NetworkConnection {

	private ArrayList<AllocationEventListener> listeners = new ArrayList<AllocationEventListener>();
	
	protected SdpPortManager sdpPortManager;

	protected NetworkConnectionBase() {
		sdpPortManager = new SdpPortManagerImpl(this);
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
	public void addListener(AllocationEventListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(AllocationEventListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public SdpPortManager getSdpPortManager() throws MsControlException {
		return sdpPortManager;
	}

	/**
	 * Gets a session template copy with all medias capacities and ports
	 * assigned.
	 * 
	 * @return
	 */
	public abstract SessionSpec generateSessionSpec();

	/**
	 * Indicates own medias and ports assigned. Warning: It could has
	 * inconsistencies with template.
	 * 
	 * @param localSpec
	 */
	public abstract void setLocalSessionSpec(SessionSpec localSpec);

	/**
	 * Indicates other party medias and ports assigned.
	 * 
	 * @return
	 */
	public abstract void setRemoteSessionSpec(SessionSpec remote);

	public abstract InetAddress getLocalAddress();

}
