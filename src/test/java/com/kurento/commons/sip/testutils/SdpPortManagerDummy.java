package com.kurento.commons.sip.testutils;

/*
 Kurento Sip User Agent implementation.
 Copyright (C) <2011>  <Tikal Technologies>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 3
 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

import javax.sdp.SdpException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.media.format.SessionSpec;
import com.kurento.commons.media.format.conversor.SdpConversor;
import com.kurento.commons.mscontrol.EventType;
import com.kurento.commons.mscontrol.MediaErr;
import com.kurento.commons.mscontrol.MediaEventListener;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManager;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerEvent;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerException;
import com.kurento.commons.sip.testutils.TestConfig.SdpPortManagerType;

public class SdpPortManagerDummy implements SdpPortManager {

	private static Logger log = LoggerFactory
			.getLogger(SdpPortManagerDummy.class);
	private int sleepTime;
	private static SdpPortManagerType sdpType;
	MediaEventListener<SdpPortManagerEvent> listener;

	public void setSdpType(SdpPortManagerType msdpType) {
		if (msdpType == null)
			sdpType = SdpPortManagerType.SDP_CORRECT;
		else if (msdpType instanceof SdpPortManagerType)
			sdpType = msdpType;
		else
			sdpType = SdpPortManagerType.SDP_CORRECT;
	}

	public void setSleepTimer(int sleepTime) {
		this.sleepTime = sleepTime;
	}

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
		SdpPortManagerEvent event = new SdpPortManagerEventDummy(
				SdpPortManagerEvent.OFFER_GENERATED, this);
		eventSleep();
		listener.onEvent(event);
	}

	@Override
	public SessionSpec getMediaServerSessionDescription()
			throws SdpPortManagerException {
		// TODO Auto-generated method stub
		return getSdp();
	}

	@Override
	public SessionSpec getUserAgentSessionDescription()
			throws SdpPortManagerException {
		return getSdp();
	}

	@Override
	public void processSdpAnswer(SessionSpec arg0)
			throws SdpPortManagerException {
		// SpecTools.intersectSessionSpec(answerer, offerer);
		SdpPortManagerEvent event = new SdpPortManagerEventDummy(
				SdpPortManagerEvent.ANSWER_PROCESSED, this);
		eventSleep();
		listener.onEvent(event);
	}

	@Override
	public void processSdpOffer(SessionSpec arg0)
			throws SdpPortManagerException {
		SdpPortManagerEvent event = new SdpPortManagerEventDummy(
				SdpPortManagerEvent.ANSWER_GENERATED, this);
		eventSleep();
		listener.onEvent(event);

	}

	@Override
	public void rejectSdpOffer() throws SdpPortManagerException {
		SdpPortManagerEvent event = new SdpPortManagerEventDummy(
				SdpPortManagerEvent.ANSWER_GENERATED, this);
		eventSleep();
		listener.onEvent(event);

	}

	private void eventSleep() {
		if (sleepTime > 0) {
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				log.error("Error while waiting to send SDP event");
			}
		}
	}

	private static String getSdpString() {
		String sdpString = "";

		if (SdpPortManagerType.SDP_CORRECT.equals(sdpType)) {
			sdpString = "v=0 \no=test 2808844564 2808844564 IN IP4 193.147.51.20 "
					+ "\ns= \nc=IN IP4 193.147.51.20 "
					+ "\nt=0 0 "
					+ "\nm=audio 49174 RTP/AVP 8"
					+ "\na=rtpmap:8 PCMA/8000"
					+ "\nm=video 49170 RTP/AVP 96 "
					+ "\na=rtpmap:96 H264/90000";
		} else if (SdpPortManagerType.SDP_ONLY_VIDEO.equals(sdpType)) {
			sdpString = "v=0 \no=test 2808844564 2808844564 IN IP4 193.147.51.20 "
					+ "\ns= \nc=IN IP4 193.147.51.20 "
					+ "\nt=0 0 "
					+ "\nm=video 49170 RTP/AVP 96 "
					+ "\na=rtpmap:96 H264/90000";
		} else if (SdpPortManagerType.SDP_ONLY_AUDIO.equals(sdpType)) {
			sdpString = "v=0 \no=test 2808844564 2808844564 IN IP4 193.147.51.20 "
					+ "\ns= \nc=IN IP4 193.147.51.20 "
					+ "\nt=0 0 "
					+ "\nm=audio 49174 RTP/AVP 8" + "\na=rtpmap:8 PCMA/8000";
		} else if (SdpPortManagerType.SDP_WITHOUT_VIDEO_AUDIO.equals(sdpType)) {
			sdpString = "v=0 \no=test 2808844564 2808844564 IN IP4 193.147.51.20 "
					+ "\ns= \nc=IN IP4 193.147.51.20 " + "\nt=0 0 ";
		} else if (SdpPortManagerType.SDP_WRONG.equals(sdpType)) {
			// audio and video wrong written
			sdpString = "v=0 \no=test 2808844564 2808844564 IN IP4 193.147.51.20 "
					+ "\ns= \nc=IN IP4 193.147.51.20 "
					+ "\nt=0 0 "
					+ "\nm=adio 49174 RTP/AVP 8"
					+ "\na=rtpmap:8 PCMA/8000"
					+ "\nm=vido 49170 RTP/AVP 96 " + "\na=rtpmap:96 H264/90000";
		} else if (SdpPortManagerType.SDP_PAYLOAD_WRONG.equals(sdpType)) {
			// Change audio payload 8 by 12
			sdpString = "v=0 \no=test 2808844564 2808844564 IN IP4 193.147.51.20 "
					+ "\ns= \nc=IN IP4 193.147.51.20 "
					+ "\nt=0 0 "
					+ "\nm=audio 49174 RTP/AVP 99"
					+ "\na=rtpmap:98 PCMA/8000"
					+ "\nm=video 49170 RTP/AVP 96 "
					+ "\na=rtpmap:96 H264/90000";
		} else if (SdpPortManagerType.SDP_EMPTY.equals(sdpType)) {
			sdpString = "";
		}

		return sdpString;
	}

	private static SessionSpec getSdp() {
		// String sdpString =
		// "v=0 \no=test 2808844564 2808844564 IN IP4 193.147.51.20 "
		// + "\ns= \nc=IN IP4 193.147.51.20 "
		// + "\nt=0 0 "
		// + "\nm=audio 49174 RTP/AVP 8"
		// + "\na=rtpmap:8 PCMA/8000"
		// + "\nm=video 49170 RTP/AVP 96 " + "\na=rtpmap:96 H264/90000";
		log.info("getSdp SdpPortManagerDummy: " + sdpType);
		String sdpString = getSdpString();
		SessionSpec sdp;

		try {
			sdp = SdpConversor.sdp2SessionSpec(sdpString);
		} catch (SdpException e) {
			log.error("Error creating test SDP", e);
			sdp = new SessionSpec();
		}

		return sdp;
	}

	private class SdpPortManagerEventDummy implements SdpPortManagerEvent {
		EventType type;
		SdpPortManager manager;

		SdpPortManagerEventDummy(EventType type, SdpPortManager manager) {
			this.type = type;
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
		public SessionSpec getMediaServerSdp() {
			return SdpPortManagerDummy.getSdp();
		}

	}

}
