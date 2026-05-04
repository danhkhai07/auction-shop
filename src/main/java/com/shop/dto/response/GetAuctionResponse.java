package com.shop.dto.response;

import com.shop.domain.AuctionStatus;
import com.shop.domain.BidTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GetAuctionResponse(
    String id,
    String name,
    BigDecimal startingPrice,
    LocalDateTime startTime,
    LocalDateTime endTime,
    AuctionStatus status,
    List<BidTransaction> bidHistory
){}
