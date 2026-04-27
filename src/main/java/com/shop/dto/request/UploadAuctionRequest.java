package com.shop.dto.request;

import com.shop.domain.AuctionStatus;
import com.shop.domain.BidTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record UploadAuctionRequest(
    String name,
    BigDecimal startingPrice,
    LocalDateTime startTime,
    LocalDateTime endTime,
    AuctionStatus status,
    List<BidTransaction> bidHistory
) {
    public boolean hasEmptyFields() {
        return (name == null || startingPrice == null || startTime == null ||
                endTime == null || status == null || bidHistory == null);
    }
}
