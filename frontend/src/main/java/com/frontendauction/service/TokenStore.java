package com.frontendauction.service;

public final class TokenStore {

    private static volatile String token;

    private TokenStore() {}

    public static void setToken(String jwt) {
        token = jwt;
    }

    public static String getToken() {
        return token;
    }

    public static boolean hasToken() {
        return token != null && !token.isBlank();
    }

    public static void clear() {
        token = null;
    }
}
