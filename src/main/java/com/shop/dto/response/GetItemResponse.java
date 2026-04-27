package com.shop.dto.response;

public class GetItemResponse {
    public final String id;
    public final String name;
    public final String description;
    public final String sellerID;

    public GetItemResponse(
            String id,
            String name,
            String description,
            String sellerID
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sellerID = sellerID;
    }
}
