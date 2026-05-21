package com.shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shop.domain.Item;

public record GetItemResponse(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("sellerID") String sellerID
){
    public GetItemResponse(Item item){
        this(item.getId(),
            item.getName(),
            item.getDescription(),
            item.getSeller().getId());
    }
}