package com.frontendauction.model;

public record LoginResult(boolean success, String token, String errorMessage) {
    public static LoginResult success(String token) {
        return new LoginResult(true, token, null);
    }
    public static LoginResult failure(String errorMessage) {
        return new LoginResult(false, null, errorMessage);
    }
}
