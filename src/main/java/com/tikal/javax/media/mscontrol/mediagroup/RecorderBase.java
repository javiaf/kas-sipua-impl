package com.tikal.javax.media.mscontrol.mediagroup;

import java.net.URI;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mediagroup.RecorderEvent;
import javax.media.mscontrol.resource.RTC;

public abstract class RecorderBase implements Recorder {

	protected MediaGroupBase parent = null;

	public RecorderBase(MediaGroupBase parent) throws MsControlException {
		this.parent = parent;
	}

	@Override
	public MediaGroup getContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addListener(MediaEventListener<RecorderEvent> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public MediaSession getMediaSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeListener(MediaEventListener<RecorderEvent> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public abstract void record(URI arg0, RTC[] arg1, Parameters arg2)
			throws MsControlException;

	@Override
	public abstract void stop();

}
