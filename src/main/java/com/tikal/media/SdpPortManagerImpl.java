package com.tikal.media;

import java.net.InetAddress;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.networkconnection.CodecPolicy;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.networkconnection.SdpPortManagerException;
import javaxt.sdp.SdpException;
import javaxt.sdp.SessionDescription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tikal.media.format.MediaSpec;
import com.tikal.media.format.SessionSpec;

public class SdpPortManagerImpl implements SdpPortManager {

	private static Log log = LogFactory.getLog(SdpPortManagerImpl.class);
	
	private List<MediaSpec> combinedMediaList;
	private NetworkConnectionBase resource;
	private SessionSpec userAgentSession; // this is remote session spec
	
	@SuppressWarnings("unchecked")
	private CopyOnWriteArrayList<MediaEventListener> mediaListenerList = new CopyOnWriteArrayList<MediaEventListener>();
	
	private SessionSpec localSpec;
	private String localAddress;

	protected SdpPortManagerImpl(NetworkConnectionBase resource) {
		this.resource = resource;
		
		InetAddress inet = resource.getLocalAddress();
		localAddress = inet.getHostAddress();
	}


	@Override
	public NetworkConnection getContainer() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void addListener(MediaEventListener<SdpPortManagerEvent> arg0) {
		mediaListenerList.add(arg0);
	}


	@Override
	public MediaSession getMediaSession() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void removeListener(MediaEventListener<SdpPortManagerEvent> arg0) {
		mediaListenerList.remove(arg0);
	}


	/**
	 * <p>
	 * Request a SDP offer from the Media Server. When complete, sends a
	 * SdpPortManagerEvent with an EventType of
	 * SdpPortManagerEvent.OFFER_GENERATED. The resulting offer is available
	 * with SdpPortManagerEvent.getMediaServerSdp() This can be used to initiate
	 * a connection, or to increase/augment the capabilities of an established
	 * connection, like for example adding a video stream to an audio-only
	 * connection.
	 * <p>
	 */
	@Override
	public void generateSdpOffer() throws SdpPortManagerException {
		SessionSpec sessionSpec = resource.generateSessionSpec();
		sessionSpec.setOriginAddress(localAddress);
		sessionSpec.setRemoteHandler(localAddress);
		SdpPortManagerEventImpl event = null;
		try {
				event = new SdpPortManagerEventImpl(SdpPortManagerEvent.OFFER_GENERATED, this, sessionSpec.getSessionDescription(), SdpPortManagerEvent.NO_ERROR);	
		} catch (SdpException e) {
			event = new SdpPortManagerEventImpl(null, this, null, SdpPortManagerEvent.RESOURCE_UNAVAILABLE );
			log.error( "Error creating Session Description from resource media list", e);
			throw new SdpPortManagerException("Error creating Session Description from resource media list",e);
		}		
		finally {
			notifyEvent(event);	
		}
	}


	@Override
	public CodecPolicy getCodecPolicy() {
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * <P>This method returns the media previously agreed after a complete offer-answer exchange. 
	 * If no media has been agreed yet, it returns null. 
	 * If an offer is in progress from either side, that offer's session description is not returned here. 
	 * <P>
	 */
	@Override
	public byte[] getMediaServerSessionDescription()
			throws SdpPortManagerException {
		SessionDescription sdp = null;
		try {
			if (localSpec != null) {
				sdp = localSpec.getSessionDescription();
			}
		} catch (SdpException e) {		
			log.error("Error creating session description.",e);
			throw new SdpPortManagerException("Error creating Session Description",e);
		}
		return sdp.toString().getBytes();
	}


	@Override
	public byte[] getUserAgentSessionDescription()
			throws SdpPortManagerException {
		SessionDescription sdp = null;
		try {
			if (userAgentSession != null) {
				sdp = userAgentSession.getSessionDescription();
			}
		} catch (SdpException e) {
			throw new SdpPortManagerException("Error creating SessionDescription", e);
		}
		return sdp.toString().getBytes();
	}


	@Override
	public void processSdpAnswer(byte[] arg0) throws SdpPortManagerException {
		SessionDescription sdp = null;
		SessionSpec ss = null;
		try {
			ss = new SessionSpec(new String(arg0));
			sdp = ss.getSessionDescription();
		} catch (SdpException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			userAgentSession = new SessionSpec(sdp);
			SessionSpec session = new SessionSpec(sdp);
			combinedMediaList = session.getMediaSpec();
			setPortToMedia(session);			
			localSpec = getPortsAssigned(session, true);
			localSpec.setOriginAddress(localAddress);
			localSpec.setRemoteHandler(localAddress);
			notifyEvent (new SdpPortManagerEventImpl(SdpPortManagerEvent.ANSWER_PROCESSED, this,null, SdpPortManagerEvent.NO_ERROR));
		} catch (SdpException e) {
			notifyEvent (new SdpPortManagerEventImpl(null, this, null, SdpPortManagerEvent.SDP_NOT_ACCEPTABLE));
			throw new SdpPortManagerException("Error getting media info",e);
		}
	}


	/**
	 * Request the MediaServer to process the given SDP offer (from the remote User Agent).
	 *	When complete, sends a SdpPortManagerEvent with an EventType of SdpPortManagerEvent.ANSWER_GENERATED.
		The resulting answer is available with SdpPortManagerEvent.getMediaServerSdp()
	 */
	@Override
	public void processSdpOffer(byte[] arg0) throws SdpPortManagerException {
		log.debug("processSdpOffer");
		SessionSpec session = null;
		SdpPortManagerEventImpl event = null;
		
		SessionDescription sdp = null;
		SessionSpec ss = null;
		try {
			ss = new SessionSpec(new String(arg0));
			sdp = ss.getSessionDescription();
		} catch (SdpException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			session = new SessionSpec(sdp);
			List<MediaSpec> cpMediaAccepted = session.getMediaSpec();
			SessionSpec serverMedias = resource.generateSessionSpec();
			combinedMediaList = combine(cpMediaAccepted, serverMedias.getMediaSpec(),false);
			
			if (combinedMediaList.isEmpty()) {
				event = new SdpPortManagerEventImpl(null, this, null, SdpPortManagerEvent.SDP_NOT_ACCEPTABLE);
			} else {
				session.setMediaSpec(combinedMediaList);
				userAgentSession = session;
				setPortToMedia(session);
				localSpec = getPortsAssigned(session, true);
				//localSpec = serverSession;
				localSpec.setOriginAddress(localAddress);
				localSpec.setRemoteHandler(localAddress);
//				event = new SdpPortManagerEventImpl(SdpPortManagerEvent.OFFER_GENERATED, this, 
//						localSpec.getSessionDescription(), SdpPortManagerEvent.NO_ERROR);
				event = new SdpPortManagerEventImpl(SdpPortManagerEvent.ANSWER_GENERATED, this, 
						localSpec.getSessionDescription(), SdpPortManagerEvent.NO_ERROR);
			}
		} catch (SdpException e) {
			event = new SdpPortManagerEventImpl(null, this, null, SdpPortManagerEvent.SDP_NOT_ACCEPTABLE);			
			log.error("Error processing SDPOffer", e);
			throw new SdpPortManagerException("Error processing SDPOffer",e);
		}
		
		notifyEvent(event);
	}


	@Override
	public void rejectSdpOffer() throws SdpPortManagerException {
		realease();
		combinedMediaList = null;
	}


	@Override
	public void setCodecPolicy(CodecPolicy arg0) throws SdpPortManagerException {
		// TODO Auto-generated method stub
	}
	
	/* ------------------------------------------------------------------------------------------------------
	 * Private methods
	 * ------------------------------------------------------------------------------------------------------
	 */
	@SuppressWarnings("unchecked")
	private void notifyEvent(SdpPortManagerEventImpl event) {

		for (MediaEventListener listener : mediaListenerList) {
			listener.onEvent(event);
		}
	}
	
	private List<MediaSpec> combine(List<MediaSpec> inputList, List<MediaSpec> resourceList, boolean changePayload) {
		//TODO resolver tema de eficiencia y payloads
		List<MediaSpec> resultList = new Vector<MediaSpec>();	
		MediaSpec media = null;
		for(MediaSpec media1 : inputList){
			for (MediaSpec media2:resourceList ) {
				media = media2.intersecPayload(media1, changePayload);
				if (media != null && !resultList.contains(media)) {
					resultList.add(media);
				}
			}
		}
		return  resultList;
	}
	
	private  void setPortToMedia(SessionSpec medias){
		resource.setRemoteSessionSpec(medias);
	}
	
	private SessionSpec getPortsAssigned(SessionSpec remote, boolean changePayload){
		SessionSpec own = resource.generateSessionSpec();
		List<MediaSpec> list = combine(own.getMediaSpec(),remote.getMediaSpec(), changePayload);
		own.setMediaSpec(list);	
		resource.setLocalSessionSpec(own);
		return own;
	}
	
	private void realease(){
		resource.release();
	}
	
}
