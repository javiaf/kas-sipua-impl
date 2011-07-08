package com.tikal.javax.media.mscontrol.mediagroup;

import java.net.URI;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.resource.RTC;

public abstract class PlayerBase implements Player {

	protected MediaGroupBase parent = null;

	public PlayerBase(MediaGroupBase parent) throws MsControlException {
		this.parent = parent;
	}

	@Override
	public MediaGroup getContainer() {
		return this.parent;
	}

	@Override
	public void addListener(MediaEventListener<PlayerEvent> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public MediaSession getMediaSession() {
		return this.parent.getMediaSession();
	}

	@Override
	public void removeListener(MediaEventListener<PlayerEvent> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public abstract void play(URI[] arg0, RTC[] arg1, Parameters arg2)
			throws MsControlException;

	@Override
	public abstract void play(URI arg0, RTC[] arg1, Parameters arg2)
			throws MsControlException;

	@Override
	public abstract void stop(boolean arg0);

}
