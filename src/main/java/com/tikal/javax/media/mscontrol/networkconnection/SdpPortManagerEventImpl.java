package com.tikal.javax.media.mscontrol.networkconnection;

import java.util.EventObject;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.Qualifier;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.resource.Trigger;
import javaxt.sdp.SessionDescription;

public class SdpPortManagerEventImpl extends EventObject implements
		SdpPortManagerEvent {

	private static final long serialVersionUID = -2136152163038746198L;

	private EventType eventType;
	private MediaErr error;
	private SessionDescription sdp;

	public SdpPortManagerEventImpl(EventType eventType, SdpPortManager source,
			SessionDescription sdp, MediaErr error) {
		super(source);
		this.eventType = eventType;
		this.source = source;
		this.sdp = sdp;
		this.error = error;
	}
	
	@Override
	public EventType getEventType() {
		return eventType;
	}
	
	@Override
	public MediaErr getError() {
		return error;
	}

	public SessionDescription getSdp() {
		return sdp;
	}

	public SdpPortManager getSource() {
		return (SdpPortManager) source;
	}

	@Override
	public Qualifier getQualifier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Trigger getRTCTrigger() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSuccessful() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte[] getMediaServerSdp() {
		if (sdp == null)
			return null;
		return sdp.toString().getBytes();
	}

}
