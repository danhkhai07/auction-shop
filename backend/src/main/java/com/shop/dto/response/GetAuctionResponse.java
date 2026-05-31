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
    @JsonProperty("description") String description,
    @JsonProperty("seller") GetShortUserResponse seller,
    @JsonProperty("startingPrice") BigDecimal startingPrice,
    @JsonProperty("minBidIncrement") BigDecimal minBidIncrement,
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
                auction.getItem().getDescription(),
                new GetShortUserResponse(auction.getItem().getSeller()),
                auction.getStartingPrice(),
                auction.getMinBidIncrement(),
                auction.getCurrentHighestPrice(),
                (auction.getCurrentHighestBidder() == null ? null
                        : auction.getCurrentHighestBidder().getId()),
                auction.getFinalPrice(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getStatus(),
                auction.getBidHistory().stream()
                        .map(BidTransactionResponse::new)
                        .collect(Collectors.toList())
        );
    }
}
