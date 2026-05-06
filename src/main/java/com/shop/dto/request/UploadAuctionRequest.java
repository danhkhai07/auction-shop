package com.shop.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shop.domain.AuctionStatus;
import com.shop.domain.BidTransaction;
import com.shop.domain.Item;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record UploadAuctionRequest(
    @JsonProperty("itemID") String itemID,
    @JsonProperty("startingPrice") BigDecimal startingPrice,
    @JsonProperty("startTime") LocalDateTime startTime,
    @JsonProperty("endTime") LocalDateTime endTime
) {
    public boolean hasEmptyFields() {
        return (itemID == null || startingPrice == null || startTime == null || endTime == null);
    }
}
