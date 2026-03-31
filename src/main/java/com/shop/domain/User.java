package com.shop.domain;

import java.util.Set;

public class User {
    public final int id;
    protected String username;
    public Set<Role> roles;

    User(int id, String username) {
        this.id = id;
        this.username = username;
    }

    public String getUsername() { return this.username; }
    protected void setUsername(String newUsername) { this.username = newUsername; }
}
