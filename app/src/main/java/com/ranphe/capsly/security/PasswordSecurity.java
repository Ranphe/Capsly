package com.ranphe.capsly.security;

import android.os.Build;
import android.util.Base64;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordSecurity {
    private static final int ITERATIONS = 210000;
    private static final int KEY_LENGTH = 256;

    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    public static String hashPassword(String password, String salt) throws GeneralSecurityException {
        String algorithm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? "PBKDF2WithHmacSHA256" : "PBKDF2WithHmacSHA1";
        char[] characters = password.toCharArray();
        PBEKeySpec specification = new PBEKeySpec(characters, Base64.decode(salt, Base64.NO_WRAP), ITERATIONS, KEY_LENGTH);
        try {
            byte[] hash = SecretKeyFactory.getInstance(algorithm).generateSecret(specification).getEncoded();
            return algorithm + "$" + ITERATIONS + "$" + Base64.encodeToString(hash, Base64.NO_WRAP);
        } finally {
            specification.clearPassword();
            Arrays.fill(characters, '\0');
        }
    }

    public static boolean verifyPassword(String password, String salt, String storedHash) throws GeneralSecurityException {
        String[] parts = storedHash.split("\\$");
        if (parts.length != 3) return false;
        String algorithm = parts[0];
        if (!algorithm.equals("PBKDF2WithHmacSHA256") && !algorithm.equals("PBKDF2WithHmacSHA1")) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations <= 0 || iterations > 1000000) return false;
            byte[] originalHash = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] saltBytes = Base64.decode(salt, Base64.NO_WRAP);
            if (originalHash.length != 32 || saltBytes.length != 16) return false;
            char[] characters = password.toCharArray();
            PBEKeySpec specification = null;
            try {
                specification = new PBEKeySpec(characters, saltBytes, iterations, originalHash.length * 8);
                byte[] calculatedHash = SecretKeyFactory.getInstance(algorithm).generateSecret(specification).getEncoded();
                return MessageDigest.isEqual(originalHash, calculatedHash);
            } finally {
                if (specification != null) specification.clearPassword();
                Arrays.fill(characters, '\0');
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}