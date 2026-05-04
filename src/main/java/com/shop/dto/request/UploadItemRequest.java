package com.shop.dto.request;

public record UploadItemRequest(
    String name,
    String description,
    String sellerID
) {
    public boolean hasEmptyFields() {
        return (name == null || description == null || sellerID == null);
    }
}