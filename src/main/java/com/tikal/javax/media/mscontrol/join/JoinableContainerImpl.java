package com.tikal.javax.media.mscontrol.join;

import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.JoinEventListener;
import javax.media.mscontrol.join.JoinableContainer;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.JoinableStream.StreamType;

public class JoinableContainerImpl extends JoinableImpl implements
		JoinableContainer {

	protected JoinableStreamBase[] streams = new JoinableStreamBase[2];

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
	public JoinableStream getJoinableStream(StreamType value)
			throws MsControlException {
		for (JoinableStreamBase s : streams) {
			if (s.getType().equals(value)) {
				return s;
			}
		}
		throw new MsControlException("Stream of type " + value
				+ " is not supported");
	}

	@Override
	public JoinableStream[] getJoinableStreams() throws MsControlException {
		return streams;
	}

}
