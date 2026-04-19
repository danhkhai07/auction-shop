package com.shop.dto.request;

public class RegisterRequest {
    public final String username;
    public final String password;
    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
