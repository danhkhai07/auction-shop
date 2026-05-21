package com.shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shop.domain.Auction;
import com.shop.domain.AuctionStatus;
import com.shop.domain.BidTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record GetAuctionResponse(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("startingPrice") BigDecimal startingPrice,
    @JsonProperty("currentHighestPrice") BigDecimal currentHighestPrice,
    @JsonProperty("currentHighestBidder") String currentHighestBidder,
    @JsonProperty("finalPrice") BigDecimal finalPrice,
    @JsonProperty("startTime") LocalDateTime startTime,
    @JsonProperty("endTime") LocalDateTime endTime,
    @JsonProperty("status") AuctionStatus status,
    @JsonProperty("bidHistory") List<BidTransactionResponse> bidHistory
){
    public GetAuctionResponse(Auction auction) {
        this(auction.getId(),
                auction.getItem().getName(),
                auction.getStartingPrice(),
                auction.getCurrentHighestPrice(),
                auction.getCurrentHighestBidder().getId(),
                auction.getFinalPrice(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getStatus(),
                auction.getBidHistory().stream()
                        .map(bid -> new BidTransactionResponse(bid))
                        .collect(Collectors.toList())
        );
    }
}
