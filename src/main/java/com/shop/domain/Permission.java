package com.shop.domain;

public enum Permission {
    VIEW_PROFILE,

    //Sản phẩm
    VIEW_ITEM,
    CREATE_ITEM,
    EDIT_ITEM,
    DELETE_ITEM,

    //Đấu giá
    VIEW_AUCTION,
    PLACE_BID,
    CANCEL_AUCTION,

    //Nâng cao
    SET_AUTO_BID,
    VIEW_BID_CHART,

    //Admin
    MANAGE_USER,
    MANAGE_AUCTIONS
}
