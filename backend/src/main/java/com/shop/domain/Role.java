package com.shop.domain;

import java.util.Set;

public enum Role {
    GUEST(Set.of(
            Permission.VIEW_ITEM,
            Permission.VIEW_AUCTION
    )),

    USER(Set.of(
            Permission.VIEW_PROFILE,
            Permission.VIEW_ITEM,
            Permission.VIEW_AUCTION,
            Permission.PLACE_BID,
            Permission.SET_AUTO_BID,
            Permission.VIEW_BID_CHART,

            //Người bán
            Permission.CREATE_ITEM,
            Permission.EDIT_ITEM,
            Permission.DELETE_ITEM,
            Permission.CANCEL_AUCTION
    )),

    ADMIN(Set.of(
            Permission.VIEW_PROFILE,
            Permission.VIEW_ITEM,
            Permission.VIEW_AUCTION,
            Permission.VIEW_BID_CHART,
            Permission.DELETE_ITEM,
            Permission.CANCEL_AUCTION,
            Permission.MANAGE_USER,
            Permission.MANAGE_AUCTIONS
    ));

    private final Set<Permission> permissions;

    Role(Set<Permission> perms) {
        this.permissions = perms;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}