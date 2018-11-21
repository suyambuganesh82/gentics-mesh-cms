package com.gentics.mesh.util;

import java.security.SecureRandom;

public final class TokenUtil {

	private static final int TOKEN_LENGHT = 12;

	/**
	 * Generate a short random token code.
	 * 
	 * @return
	 */
	public static String randomToken() {

		String chars = "abcdefghjkmnpqrstuvwxyz";
		String nums = "23456789";
		String passSymbols = chars + nums + chars.toUpperCase();

		SecureRandom random = new SecureRandom();

		char[] password = new char[TOKEN_LENGHT];
		for (int i = 0; i < TOKEN_LENGHT; i++) {
			password[i] = passSymbols.charAt(random.nextInt(passSymbols.length()));
		}
		return new String(password);
	}

}
