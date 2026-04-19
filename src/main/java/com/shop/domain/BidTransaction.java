package com.shop.domain;

import java.time.LocalDateTime;

public class BidTransaction {
    private String transactionId;
    private User bidder;
    private double bidAmount;
    private LocalDateTime timestamp;

    public BidTransaction(String transactionId, User bidder, double bidAmount) {
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

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}