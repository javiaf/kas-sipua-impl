package com.kurento.commons.sip.util;

import gov.nist.javax.sip.ListeningPointExt;

import java.io.IOException;
import javax.sip.ListeningPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.ua.exception.ServerInternalErrorException;
import com.kurento.commons.ua.timer.KurentoUaTimer;
import com.kurento.commons.ua.timer.KurentoUaTimerTask;

public class NatKeepAlive {

	private static final Logger log = LoggerFactory
			.getLogger(NatKeepAlive.class);

	private String proxyAddr;
	private int proxyPort;
	ListeningPointExt listeningPointImpl;
	KurentoUaTimer timer;
	private long delay = 5000;

	public NatKeepAlive(SipConfig config, ListeningPoint listeningPoint) throws ServerInternalErrorException {
		proxyAddr = config.getProxyAddress();
		proxyPort = config.getProxyPort();
		delay = config.getKeepAlivePeriod();
		log.debug("Delay for hole punching setted as " + delay);
		listeningPointImpl = (ListeningPointExt) listeningPoint;
		timer = config.getTimer();
		if (timer == null)
			throw new ServerInternalErrorException(
					"A timer must be configured in SipConfig in order to activate SIP keep-alive there must be");
	}

	private KurentoUaTimerTask task = new KurentoUaTimerTask() {

		@Override
		public void run() {
			log.debug("Sending keep alive");
			try {
				listeningPointImpl.sendHeartbeat(proxyAddr, proxyPort);
			} catch (IOException e) {
				log.error("Unable to send SIP keep-alive message", e);
			}

		}
	};

	public void start() {
		timer.schedule(task, delay, delay);
	}

	public void stop() {
		timer.cancel(task);
	}

}
