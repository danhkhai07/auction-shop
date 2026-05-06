package com.shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GetItemResponse(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("sellerID") String sellerID
){}