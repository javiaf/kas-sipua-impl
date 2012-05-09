package com.kurento.commons.sip.util;

import gov.nist.javax.sip.ListeningPointExt;

import java.io.IOException;
import javax.sip.ListeningPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public NatKeepAlive(SipConfig config, ListeningPoint listeningPoint) {
		proxyAddr = config.getProxyAddress();
		proxyPort = config.getProxyPort();
		delay = config.getKeepAlivePeriod();
		log.debug("Delay for  hole punching setted as " + delay);
		listeningPointImpl = (ListeningPointExt) listeningPoint;
		timer = config.getTimer();
	}

	private KurentoUaTimerTask task = new KurentoUaTimerTask() {

		@Override
		public void run() {
			try {
				log.debug("Sending keep alive");
				listeningPointImpl.sendHeartbeat(proxyAddr, proxyPort);
			} catch (IOException e) {

			}

		}
	};

	public void start() {
		timer.schedule(task, 0, delay);
	}

	public void stop() {
		timer.cancel(task);
	}

}
