package com.tikal.sip.util;

import java.util.Hashtable;
import java.util.Random;

public class SipHeaderHelper {

	static Hashtable<Integer, Boolean> usedPorts = new Hashtable<Integer, Boolean>();

	private static Random rn = new Random(System.currentTimeMillis());
	private static int tagLength = 10;
	private static int branchLength = 10;

	public static String getNewRandomTag() {
		byte t[] = new byte[tagLength];
		for (int i = 0; i < tagLength; i++) {
			t[i] = (byte) rand('a', 'z');
		}
		return new String(t);
	}

	public static String getNewRandomBranch() {
		byte b[] = new byte[branchLength];
		for (int i = 0; i < branchLength; i++) {
			b[i] = (byte) rand('a', 'z');
		}
		return "z9hG4bK_" + new String(b);
	}

	private static int rand(int lo, int hi) {
		int n = hi - lo + 1;
		int i = rn.nextInt(n);
		if (i < 0) {
			i = -i;
		}
		return lo + i;
	}
}
