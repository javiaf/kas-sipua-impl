package com.kurento.commons.sip.testutils;

public class TestErrorException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a ServerInternalErrorException
	 * 
	 * @param message
	 *            The message with the Exception
	 */
	public TestErrorException(String message) {
		super(message);
	}

	/**
	 * Constructs a ServerInternalErrorException
	 * 
	 * @param message
	 *            The message with the Exception
	 * @param cause
	 *            The cause of the Exception
	 */
	public TestErrorException(String message, Throwable cause) {
		super(message, cause);
	}

}
