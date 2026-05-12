package com.shop.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UploadItemRequest(
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("sellerID") String sellerID
) {
    public boolean hasEmptyFields() {
        return (name == null || description == null || sellerID == null);
    }
}