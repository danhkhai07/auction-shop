package com.shop.dto.request;

import com.shop.domain.AuctionStatus;
import com.shop.domain.BidTransaction;
import com.shop.domain.Item;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record UploadAuctionRequest(
    String itemID,
    BigDecimal startingPrice,
    LocalDateTime startTime,
    LocalDateTime endTime
) {
    public boolean hasEmptyFields() {
        return (startingPrice == null || startTime == null || endTime == null);
    }
}
