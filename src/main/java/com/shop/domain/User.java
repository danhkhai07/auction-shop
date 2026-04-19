package com.shop.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class User {
    private final String id;
    private String username;
    private String passwordHash;
    private Set<Role> roles = new HashSet<>();

    public User(String id, String username) {
        this.id = id;
        this.username = username;
    }
    public String getId() {
        return id;
    }

    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }
    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public boolean hasPermission(Permission permission) {
        for (Role role : roles) {
            if (role.getPermissions().contains(permission)) return true;
        }
        return false;
    }
    public Set<Permission> getPermissions() {
        Set<Permission> perms = new HashSet<>();
        for (Role role : roles) {
            perms.addAll(role.getPermissions());
        }
        return perms;
    }
}
