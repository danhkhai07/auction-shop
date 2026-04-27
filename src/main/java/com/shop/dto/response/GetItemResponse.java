package com.shop.dto.response;

public record GetItemResponse(
    String id,
    String name,
    String description,
    String sellerID
){}