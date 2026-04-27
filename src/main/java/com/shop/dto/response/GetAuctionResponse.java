package com.shop.dto.response;

import com.shop.domain.AuctionStatus;
import com.shop.domain.BidTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class GetAuctionResponse {
    public final String id;
    public final String name;
    public final BigDecimal startingPrice;
    public final LocalDateTime startTime;
    public final LocalDateTime endTime;
    public final AuctionStatus status;
    public final List<BidTransaction> bidHistory;

    public GetAuctionResponse(
            String id,
            String name,
            BigDecimal startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AuctionStatus status,
            List<BidTransaction> bidHistory
    ) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.bidHistory = bidHistory;
    }
}
