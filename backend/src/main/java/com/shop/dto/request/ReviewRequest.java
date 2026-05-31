package com.shop.dto.request;

public record ReviewRequest(
        String targetUsername,
        Integer stars,
        String comment
) {
}
