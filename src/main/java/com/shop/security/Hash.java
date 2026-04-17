package com.shop.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {
    public static String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return toHex(encodedHash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean compareHash(String text, String hash) {
        return Hash.hash(text).equals(hash);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append(0);
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
