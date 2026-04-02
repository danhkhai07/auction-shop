package com.shop.domain;

import java.util.Set;

public enum Role {
    GUEST(Set.of(
            Permission.VIEW_ITEM
    )),
    USER(Set.of(
            Permission.VIEW_ITEM,
            Permission.CREATE_ITEM,
            Permission.DELETE_ITEM
    )),
    ADMIN(Set.of(
            Permission.VIEW_ITEM,
            Permission.CREATE_ITEM,
            Permission.DELETE_ITEM,
            Permission.MANAGE_USER,
            Permission.BAN_USER
    ));

    private final Set<Permission> permissions;

    Role(Set<Permission> perms) {
        this.permissions = perms;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}