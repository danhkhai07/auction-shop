package com.shop.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptHash {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public static String hash(String text) {
        return encoder.encode(text);
    }

    public static boolean compareHash(String text, String hash) {
        return encoder.matches(text, hash);
    }
}
