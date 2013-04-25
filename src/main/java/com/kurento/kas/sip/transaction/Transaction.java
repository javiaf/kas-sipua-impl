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

package com.kurento.kas.sip.transaction;

import java.security.SecureRandom;

public abstract class Transaction {

	static long cSeqNumber = System.currentTimeMillis() % 100000000;

	// Protocol helper
	private static SecureRandom rn = new SecureRandom();
	private static final int tagLength = 10;
	private static final int branchLength = 10;

	// ///////////////
	//
	// SIP HELPERS
	//
	// ///////////////

	String getNewRandomTag() {
		byte t[] = new byte[tagLength];
		for (int i = 0; i < tagLength; i++) {
			t[i] = (byte) rand('a', 'z');
		}
		return new String(t);
	}

	String getNewRandomBranch() {
		byte b[] = new byte[branchLength];
		for (int i = 0; i < branchLength; i++) {
			b[i] = (byte) rand('a', 'z');
		}
		return "z9hG4bK_" + new String(b);
	}

	int rand(int lo, int hi) {
		int n = hi - lo + 1;
		int i = rn.nextInt(n);
		if (i < 0) {
			i = -i;
		}
		return lo + i;
	}

}