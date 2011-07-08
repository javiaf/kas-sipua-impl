package com.tikal.javax.media.mscontrol.join;

import javax.media.mscontrol.join.JoinableContainer;
import javax.media.mscontrol.join.JoinableStream;

public abstract class JoinableStreamBase extends JoinableImpl implements JoinableStream {

	private JoinableContainer container;
	private StreamType type;
	
	protected JoinableStreamBase(JoinableContainer container, StreamType type) {
		this.container = container;
		this.type = type;
	}
	
	@Override
	public JoinableContainer getContainer() {
		return this.container;
	}

	@Override
	public StreamType getType() {
		return this.type;
	}

}
