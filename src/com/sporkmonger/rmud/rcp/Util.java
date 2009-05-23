package com.sporkmonger.rmud.rcp;

public class Util {
	public static boolean isMac() {
		String osName = System.getProperty("os.name", "");
		return osName.equals("Mac OS X");
	}
}
