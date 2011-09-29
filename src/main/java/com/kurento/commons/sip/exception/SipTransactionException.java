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
package com.kurento.commons.sip.exception;

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
