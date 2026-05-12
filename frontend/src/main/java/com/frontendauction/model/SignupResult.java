package com.frontendauction.model;

public record SignupResult(boolean success, String message, String errorMessage) {

    public static SignupResult success(String message) {
        return new SignupResult(true, message, null);
    }

    public static SignupResult failure(String errorMessage) {
        return new SignupResult(false, null, errorMessage);
    }
}
