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
package com.kurento.commons.sip.testutils;

import java.util.EventObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventListener {

	private static final Logger log = LoggerFactory.getLogger(SipEndPointController.class);

	private String id;
	private BlockingQueue<EventObject> eventFifo = new LinkedBlockingQueue<EventObject>();

	public EventListener(String id) {
		this.id = id;
	}

	// Listener Interface
	public <T extends EventObject> void onEvent(T event) {
		log.info("TEST FACILITY received an event:" + event);
		try {
			eventFifo.put(event);
		} catch (InterruptedException e) {
			log.error("Unable to insert event into FIFO", e);
		}
	}

	// TestInterface
	@SuppressWarnings("unchecked")
	public <T extends EventObject> T poll(int timeoutSec)
			throws InterruptedException {
		log.info(id + " - TEST FACILITY polling. Asking for new event. Wait until reception");
		T e = (T) eventFifo.poll(timeoutSec, TimeUnit.SECONDS);
		log.info(id + " - TEST FACILITY polling. Found new event, response: " + e);
		return e;
	}

}
