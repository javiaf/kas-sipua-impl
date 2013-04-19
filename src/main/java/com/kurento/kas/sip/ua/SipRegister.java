package com.kurento.kas.sip.ua;

import java.util.UUID;

import com.kurento.kas.ua.Register;

public class SipRegister extends Register{

	private long cseq;
	private UUID registerCallId;

	public SipRegister(String user, String realm) {
		super(user,realm);
		// instance-id: RFC5626. Used to set the registrarCallId
		/*
		 * According to RFC5626 instance-id must stay the same on UA reboot or
		 * power cycle. This implementation assigns temporal UUID that stays the
		 * same during UA's life cycle
		 */

		this.registerCallId = UUID.randomUUID();
		this.cseq = 1;
	}

	public synchronized long getCseq() {
		return cseq++;
	}

	public String getRegisterCallId() {
		return registerCallId.toString();
	}

}
