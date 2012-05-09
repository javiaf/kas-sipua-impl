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
package com.kurento.commons.sip.junit;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BugTest {
	private final static Logger log = LoggerFactory
			.getLogger(RegisterTest.class);

	@BeforeClass
	public static void initTest() throws Exception {
		// TODO
	}

	/**
	 * Ticket #42
	 * 
	 * C:---INVITE---------->:S<br>
	 * C:<----------200 OK---:S<br>
	 * C:---ACK------------->:S<br>
	 * C1:---INVITE--------->:S<br>
	 * C1:<--BUSY------------:S<br>
	 * C1:----------200 OK-->:S<br>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReceiveInviteDuringCall() throws Exception {
		// TODO
	}

	/**
	 * Ticket #191
	 * 
	 * C:---INVITE---------->:S<br>
	 * C:<----------INVITE---:S<br>
	 * 
	 * @throws Exception
	 */
	// TODO Define the messages correctly
	@Test
	public void testSendInviteAndReceiveInvite() throws Exception {
		// TODO
	}

	/**
	 * Ticket #217
	 * 
	 * @throws Exception
	 */
	// TODO Define the messages correctly
	@Test
	public void testSendCancelAfterInviteCrash() throws Exception {
		// TODO
	}

	/**
	 * Ticket #235
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSendRegisterAfterExpires() throws Exception {
		// TODO Fixed at RegisterTest
	}

	// FIXED at Cancel Test
	/**
	 * Ticket #187
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSendInviteAndSendCancel() throws Exception {
		// TODO Fixed at CancelTest
	}

	/**
	 * Ticket #203
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSendInviteAndCancelCrossAccept() throws Exception {
		// TODO Fixed at CancelTest
	}


}
