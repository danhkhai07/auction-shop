package com.shop.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ExtendAuctionTime(
    @JsonProperty("newEndTime") LocalDateTime newEndTime
) {}
