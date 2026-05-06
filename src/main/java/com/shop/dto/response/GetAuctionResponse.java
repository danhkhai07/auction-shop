package com.shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shop.domain.AuctionStatus;
import com.shop.domain.BidTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GetAuctionResponse(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("startingPrice") BigDecimal startingPrice,
    @JsonProperty("startTime") LocalDateTime startTime,
    @JsonProperty("endTime") LocalDateTime endTime,
    @JsonProperty("status") AuctionStatus status,
    @JsonProperty("bidHistory") List<BidTransaction> bidHistory
){}
