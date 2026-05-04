package com.shop.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class User {
    public final String id;
    public String username;
    public String passwordHash;
    private Set<Role> roles = new HashSet<>();

    private List<Item> ownedItems = new ArrayList<>();
    private List<Auction> ownedAuctions = new ArrayList<>();

    public User(String id, String username) {
        this.id = id;
        this.username = username;
    }

    // =================================================================================================================
    //                  HÀM CHÍNH
    // =================================================================================================================

    public void addItem(Item item) {
        if (item != null) {
            this.ownedItems.add(item);
        }
    }

    public void addAuction(Auction auction) {
        if (auction != null) {
            this.ownedAuctions.add(auction);
        }
    }

    public void addRole(Role role) {
        if (role != null) {
            this.roles.add(role);
        }
    }

    public boolean hasPermission(Permission permission) {
        for (Role role : roles) {
            if (role.getPermissions().contains(permission)) return true;
        }
        return false;
    }

    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    // =================================================================================================================
    //                  HELPER
    // =================================================================================================================

    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    public boolean isRegularUser() {
        return hasRole(Role.USER);
    }

    public boolean isGuest() {
        return hasRole(Role.GUEST);
    }

    public boolean hasItem() {
        return !ownedItems.isEmpty();
    }

    public int getItemCount() {
        return ownedItems.size();
    }

    // =================================================================================================================
    //                  GETTERS
    // =================================================================================================================

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<Permission> getPermissions() {
        Set<Permission> perms = new HashSet<>();
        for (Role role : roles) {
            perms.addAll(role.getPermissions());
        }
        return perms;
    }

    public List<Item> getOwnedItems() {
        return Collections.unmodifiableList(ownedItems);
    }

    public List<Auction> getOwnedAuctions() {
        return Collections.unmodifiableList(ownedAuctions);
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    // =================================================================================================================
    //                  SETTERS
    // =================================================================================================================

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        this.username = username.trim();
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash không được để trống");
        }
        this.passwordHash = passwordHash;
    }

    public void setRoles(Set<Role> roles) {
        if (roles != null) {
            this.roles = new HashSet<>(roles);
        } else {
            this.roles = new HashSet<>();
        }
    }
}

