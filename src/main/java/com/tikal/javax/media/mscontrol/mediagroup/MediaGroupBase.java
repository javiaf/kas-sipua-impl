package com.tikal.javax.media.mscontrol.mediagroup;

import java.net.URI;
import java.util.Iterator;

import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalGenerator;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.AllocationEventListener;

import com.tikal.javax.media.mscontrol.join.JoinableContainerImpl;

public abstract class MediaGroupBase extends JoinableContainerImpl implements
		MediaGroup {
	
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
	public Player getPlayer() throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Recorder getRecorder() throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SignalDetector getSignalDetector() throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SignalGenerator getSignalGenerator() throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
