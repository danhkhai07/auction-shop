package com.shop.dto.event;

import com.shop.domain.Auction;
import com.shop.domain.AuctionStatus;
import com.shop.domain.User;
import com.shop.dto.response.GetShortUserResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AuctionEvent(
        String type,
        AuctionStatus status,
        BigDecimal currentHighestPrice,
        GetShortUserResponse currentHighestBidder,
        BigDecimal finalPrice,
        LocalDateTime endTime
) {
    public AuctionEvent(String type, Auction auction) {
        this(type,
            auction.getStatus(),
            auction.getCurrentHighestPrice(),
            new GetShortUserResponse(auction.getCurrentHighestBidder()),
            auction.getFinalPrice(),
            auction.getEndTime()
        );
    }
}
