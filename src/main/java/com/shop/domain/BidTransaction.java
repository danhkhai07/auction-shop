package com.shop.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidTransaction {
    private final User bidder;
    public final String transactionId;
    public final BigDecimal bidAmount;
    public final LocalDateTime timestamp;

    public BidTransaction(String transactionId, User bidder, BigDecimal bidAmount) {
        if (transactionId == null || transactionId.isBlank())
            throw new IllegalArgumentException("Transaction ID is required.");
        if (bidder == null)
            throw new IllegalArgumentException("Bidder is required.");
        if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("The bid amount must be greater than 0.");
        this.transactionId = transactionId;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public User getBidder() {
        return bidder;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}