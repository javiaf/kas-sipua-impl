package com.kurento.commons.sip.event;

import java.util.EventObject;

import com.kurento.ua.commons.UA;

public class SipEvent extends EventObject {

	private static final long serialVersionUID = 5160240637095464614L;

	private SipEventEnum eventType;
	private String method;
	private int code;
	private String branchId;

	public static final SipEventEnum REQUEST = SipEventEnum.REQUEST;
	public static final SipEventEnum RESPONSE = SipEventEnum.RESPONSE;

	public SipEvent(UA source, String method, String branchId) {
		super(source);
		this.eventType = REQUEST;
		this.method = method;
		this.branchId = branchId;
	}
	
	public SipEvent(UA source, int code, String branchId){
		super(source);
		this.eventType = REQUEST;
		this.code = code;
		this.branchId = branchId;
	
	}
	
	public SipEventEnum getEventType(){
		return eventType;
	}
	
	public String getMethod(){
		return method;
	}
	
	public int getCode() {
		return code;
	}
	
	public String getBranchId(){
		return branchId;
	}

}
