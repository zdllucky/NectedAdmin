package app.entities;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Salt {
	public static byte[] getSalt() throws NoSuchAlgorithmException {
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		for (int i = 0; i < 16; i++) {
			System.out.print(salt[i] & 0x00FF);
			System.out.print(" ");
		}
		return salt;
	}
}