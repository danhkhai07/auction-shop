package com.shop.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidTransaction {
    private String transactionId;
    private User bidder;
    private BigDecimal bidAmount;
    private LocalDateTime timestamp;

    public BidTransaction(String transactionId, User bidder, BigDecimal bidAmount) {
        if (transactionId == null)
            throw new IllegalArgumentException("Transaction ID không được để trống.");
        if (bidder == null)
            throw new IllegalArgumentException("Phải có người đấu giá.");
        if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Số tiền đặt giá phải lớn hơn 0.");
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