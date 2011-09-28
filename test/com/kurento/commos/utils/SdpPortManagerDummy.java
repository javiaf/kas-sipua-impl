package com.kurento.commos.utils;

import com.kurento.commons.mscontrol.EventType;
import com.kurento.commons.mscontrol.MediaErr;
import com.kurento.commons.mscontrol.MediaEventListener;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManager;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerException;

public class SdpPortManagerDummy implements SdpPortManager {
	
	MediaEventListener<SdpPortManagerEvent> listener;

	@Override
	public NetworkConnection getContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addListener(MediaEventListener<SdpPortManagerEvent> arg0) {
		listener = arg0;

	}

	@Override
	public void removeListener(MediaEventListener<SdpPortManagerEvent> arg0) {
		listener = null;

	}

	@Override
	public void generateSdpOffer() throws SdpPortManagerException {
		SdpPortManagerEvent event = new  SdpPortManagerEventDummy(SdpPortManagerEvent.OFFER_GENERATED, this); 
		listener.onEvent(event);
	}

	@Override
	public byte[] getMediaServerSessionDescription()
			throws SdpPortManagerException {
		// TODO Auto-generated method stub
		return getSdp();
	}

	@Override
	public byte[] getUserAgentSessionDescription()
			throws SdpPortManagerException {
		return getSdp();
	}

	@Override
	public void processSdpAnswer(byte[] arg0) throws SdpPortManagerException {
		//SpecTools.intersectSessionSpec(answerer, offerer);
		SdpPortManagerEvent event = new  SdpPortManagerEventDummy(SdpPortManagerEvent.ANSWER_PROCESSED, this); 
		listener.onEvent(event);
	}

	@Override
	public void processSdpOffer(byte[] arg0) throws SdpPortManagerException {
		SdpPortManagerEvent event = new  SdpPortManagerEventDummy(SdpPortManagerEvent.ANSWER_GENERATED, this); 
		listener.onEvent(event);

	}

	@Override
	public void rejectSdpOffer() throws SdpPortManagerException {
		SdpPortManagerEvent event = new  SdpPortManagerEventDummy(SdpPortManagerEvent.ANSWER_GENERATED, this); 
		listener.onEvent(event);

	}
	
	private static byte[] getSdp() {
		String sdpString =  "v=0 \no=test 2808844564 2808844564 IN IP4 193.147.51.20 " +
		"\ns= \nc=IN IP4 193.147.51.20 " +
		"\nt=0 0 " +
		"\nm=audio 49174 RTP/AVP 8" +
		"\na=rtpmap:8 PCMA/8000" +
		"\nm=video 49170 RTP/AVP 96 " +
		"\na=rtpmap:96 H264/90000";
		return sdpString.getBytes();
	}
	
	private class SdpPortManagerEventDummy implements SdpPortManagerEvent {
		EventType type;
		SdpPortManager manager;
		SdpPortManagerEventDummy(EventType type, SdpPortManager manager) {
			this.type  = type;
			this.manager = manager;
		}
		@Override
		public MediaErr getError() {
			return MediaErr.NO_ERROR;
		}
		@Override
		public String getErrorText() {
			return null;
		}
		@Override
		public EventType getEventType() {
			return type;
		}
		@Override
		public SdpPortManager getSource() {
			return manager;
		}

		@Override
		public boolean isSuccessful() {
			return true;
		}

		@Override
		public byte[] getMediaServerSdp() {

			return SdpPortManagerDummy.getSdp();
		}
		
	}

}
