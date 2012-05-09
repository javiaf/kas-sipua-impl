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
	 * Ticket #187
	 * 
	 * C:---INVITE---------->:S<br>
	 * C:----CANCEL--------->:S<br>
	 * 
	 * @throws Exception
	 */
	// TODO Define the messages correctly
	@Test
	public void testSendInviteAndSendCancel() throws Exception {
		// TODO
	}

	/**
	 * Ticket #203
	 * 
	 * C:---INVITE---------->:S<br>
	 * C:----CANCEL------>X<---200 OK------:S<br>
	 * 
	 * @throws Exception
	 */
	// TODO Define the messages correctly
	@Test
	public void testSendInviteAndCancelCrossAccept() throws Exception {
		// TODO
	}

	/**
	 * Ticket #217
	 * 
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
	// TODO Define the messages correctly
	@Test
	public void testSendRegisterAfterExpires() throws Exception {
		// TODO
	}

	/**
	 * Ticket #287
	 * 
	 * 
	 * @throws Exception
	 */
	// TODO Define the messages correctly
	@Test
	public void testReceive200OkAndBye() throws Exception {
		// TODO
	}

}
