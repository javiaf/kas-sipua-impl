package com.kurento.commons.sip.util;

import gov.nist.javax.sip.ListeningPointExt;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.sip.ListeningPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NatKeepAlive {

	private static final Logger log = LoggerFactory
			.getLogger(NatKeepAlive.class);

	private String proxyAddr;
	private int proxyPort;
	ListeningPointExt listeningPointImpl;
	Timer timer = new Timer();
	private long delay = 5000;

	public NatKeepAlive(SipConfig config, ListeningPoint listeningPoint) {
		proxyAddr = config.getProxyAddress();
		proxyPort = config.getProxyPort();
		delay = config.getKeepAlivePeriod();
		log.debug("Delay for  hole punching setted as " + delay);
		listeningPointImpl = (ListeningPointExt) listeningPoint;

	}

	private TimerTask timertask = new TimerTask() {

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
		timer.schedule(timertask, 0, delay);
	}

	public void stop() {
		timer.cancel();
	}

}
