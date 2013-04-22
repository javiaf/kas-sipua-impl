package com.kurento.kas.sip.ua;

public class KurentoSipException extends Exception {

	private static final long serialVersionUID = -8005123375549681709L;

	public KurentoSipException(String msg) {
		super(msg);
	}

	public KurentoSipException(String msg, Throwable e) {
		super(msg, e);
	}

}
