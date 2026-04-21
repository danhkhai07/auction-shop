package com.shop.dto.response;

import com.shop.domain.Item;

public class MeResponse {
    public String id;
    public String username;
    public Item[] listings;

    MeResponse(String id, String username, Item[] listings) {
        this.id = id;
        this.username = username;
        this.listings = listings;
    }
}
