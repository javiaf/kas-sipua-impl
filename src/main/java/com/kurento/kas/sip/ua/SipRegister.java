package com.kurento.kas.sip.ua;

import java.util.UUID;

import javax.sip.address.Address;

import com.kurento.kas.ua.Register;

public class SipRegister {

	private Register register;
	private long cseq;
	private UUID registerCallId;
	private Address address;

	public SipRegister(Register register, Address address) {
		// instance-id: RFC5626. Used to set the registrarCallId
		/*
		 * According to RFC5626 instance-id must stay the same on UA reboot or
		 * power cycle. This implementation assigns temporal UUID that stays the
		 * same during UA's life cycle
		 */

		this.register = register;
		this.address = address;
		this.registerCallId = UUID.randomUUID();
		this.cseq = 1;
	}

	public Register getRegister() {
		return this.register;
	}

	public Address getAddress() {
		return this.address;
	}

	public synchronized long getCseq() {
		return cseq++;
	}

	public String getRegisterCallId() {
		return registerCallId.toString();
	}

}
