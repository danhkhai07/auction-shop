package com.shop.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Set;

public class User {
    private final String id;
    private String username;
    private String passwordHash;
    private BigDecimal balance = BigDecimal.ZERO;
    private boolean banned;
    private String bannedReason;
    private LocalDateTime bannedAt;
    private String bannedBy;
    private Set<Role> roles = new HashSet<>();

    private List<Item> ownedItems = new ArrayList<>();
    private List<Auction> ownedAuctions = new ArrayList<>();

    public User(String id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.balance = BigDecimal.ZERO;
        this.roles.add(Role.USER);
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

    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: " + this.balance + ", Requested: " + amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    public void deductFromBalance(BigDecimal amount) {
        withdraw(amount);
    }

    public void addToBalance(BigDecimal amount) {
        deposit(amount);
    }

    public boolean hasEnoughBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
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

    public void ban(String reason, String bannedBy) {
        this.banned = true;
        this.bannedReason = reason;
        this.bannedBy = bannedBy;
        this.bannedAt = LocalDateTime.now();
    }

    public void unban() {
        this.banned = false;
        this.bannedReason = null;
        this.bannedAt = null;
        this.bannedBy = null;
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

    public boolean isBanned() {
        return banned;
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

    public BigDecimal getBalance() {
        return balance;
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

    public List<String> getOwnedItemIds() {
        return ownedItems.stream().map(Item::getId).toList();
    }

    public List<String> getOwnedAuctionIds() {
        return ownedAuctions.stream().map(Auction::getId).toList();
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public String getBannedReason() {
        return bannedReason;
    }

    public LocalDateTime getBannedAt() {
        return bannedAt;
    }

    public String getBannedBy() {
        return bannedBy;
    }

    // =================================================================================================================
    //                  SETTERS
    // =================================================================================================================

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        this.username = username.trim();
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be blank");
        }
        this.passwordHash = passwordHash;
    }

    public void setBalance(BigDecimal balance) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        this.balance = balance;
    }

    public void setRoles(Set<Role> roles) {
        if (roles != null) {
            this.roles = new HashSet<>(roles);
        } else {
            this.roles = new HashSet<>();
        }
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public void setBannedReason(String bannedReason) {
        this.bannedReason = bannedReason;
    }

    public void setBannedAt(LocalDateTime bannedAt) {
        this.bannedAt = bannedAt;
    }

    public void setBannedBy(String bannedBy) {
        this.bannedBy = bannedBy;
    }
}
