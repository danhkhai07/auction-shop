package com.shop.dto.response;

import com.shop.domain.Item;

public class MeResponse {
    public String id;
    public String username;
//    public Item[] listings;

    public MeResponse(String id, String username) {
        this.id = id;
        this.username = username;
//        this.listings = listings;
    }
}
