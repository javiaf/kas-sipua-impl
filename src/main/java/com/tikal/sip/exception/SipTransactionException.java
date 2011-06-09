package com.tikal.sip.exception;

public class SipTransactionException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a ServerInternalErrorException
	 * 
	 * @param message
	 *            The message with the Exception
	 */
	public SipTransactionException(String message) {
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
	public SipTransactionException(String message, Throwable cause) {
		super(message, cause);
	}

}
