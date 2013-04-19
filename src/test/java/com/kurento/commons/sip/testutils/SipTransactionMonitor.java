package com.kurento.commons.sip.testutils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.commons.sip.event.SipEvent;
import com.kurento.commons.sip.event.SipEventEnum;
import com.kurento.kas.sip.ua.UaMessageListener;

public class SipTransactionMonitor implements UaMessageListener {
	
	private static final Logger log = LoggerFactory.getLogger(SipTransactionMonitor.class);

	private String id;
	private BlockingQueue<SipEvent> eventFifo = new LinkedBlockingQueue<SipEvent>();

	public SipTransactionMonitor(String id) {
		this.id = "SipTransaction-"+id;
	}

	public SipEvent pollEvent(int timeoutSec)
			throws InterruptedException {
		
		log.info(id + " - TEST FACILITY polling. Asking for new SipEvent. Wait until reception");
		SipEvent e = eventFifo.poll(timeoutSec, TimeUnit.SECONDS);

		String msg = id + " - TEST FACILITY polling. ";
		if (SipEventEnum.REQUEST.equals(e.getEventType()))
			msg += "Found new REQUEST: " + e.getMethod();
		else
			msg += "Found new RESPONSE: " + e.getCode();
		log.info(msg);
		return e;
	}


	@Override
	public void onEvent(SipEvent event) {
		String msg = id + " - TEST FACILITY ";
		if (SipEventEnum.REQUEST.equals(event.getEventType()))
			msg += "received REQUEST: " + event.getMethod();
		else
			msg += "receive RESPONSE: " + event.getCode();
		
		log.info(msg);
		try {
			eventFifo.put(event);
		} catch (InterruptedException e) {
			log.error("Unable to insert event into FIFO", e);
		}
	}
}
