package com.shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shop.domain.BidTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidTransactionResponse(
    @JsonProperty("bidder") GetUserResponse bidder,
    @JsonProperty("transactionId") String transactionId,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("timestamp") LocalDateTime timestamp
){
    public BidTransactionResponse(BidTransaction bid) {
        this(new GetUserResponse(bid.getBidder()), bid.getTransactionId(), bid.getBidAmount(), bid.getTimestamp());
    }
}
