package com.shop.dto.request;

import java.time.LocalDateTime;

public record ExtendAuctionTime(
    LocalDateTime newEndTime
) {}
