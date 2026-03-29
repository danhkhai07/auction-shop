package com.shop.domain;

public abstract class User {
    public final int id;
    protected String username;

    User(int id, String username) {
        this.id = id;
        this.username = username;
    }

    public String getUsername() { return this.username; }
    protected void setUsername(String newUsername) { this.username = newUsername; }
}
