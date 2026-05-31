package com.shop.dto.response;

import java.time.Instant;

public record ReviewResponse(
        String id,
        String reviewerUsername,
        String targetUsername,
        Integer stars,
        String comment,
        Instant createdAt
) {
}
