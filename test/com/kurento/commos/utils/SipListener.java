package com.kurento.commos.utils;

import java.util.EventObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class SipListener {
	private static final Log log = LogFactory.getLog(SipListener.class);
	
	private BlockingQueue<EventObject> eventFifo = new LinkedBlockingQueue<EventObject>();
	
	
	/////// Listener Interface ///////
	public <T extends EventObject> void onEvent( T event) {
		log.debug("TEST FACILITY received and event:" + event);
		try {
			eventFifo.put(event);
		} catch (InterruptedException e) {
			log.error("Unable to insert event into FIFO",e);
		}
	}

	/////// TestInterface ///////
	@SuppressWarnings("unchecked")
	public <T extends EventObject> T poll(int timeoutSec) throws InterruptedException {
		log.debug("TEST FACILITY polling. Asking for new event. Wait until reception");
		T e = (T) eventFifo.poll(timeoutSec, TimeUnit.SECONDS);
		log.debug("TEST FACILITY polling. Found new event, response:" + e );
		return e;

	}
}
