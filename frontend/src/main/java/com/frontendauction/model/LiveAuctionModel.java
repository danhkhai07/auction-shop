package com.frontendauction.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LiveAuctionModel {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuctionDetail {
        private String id;
        private String name;
        private Double startingPrice;
        private String startTime;
        private String endTime;
        private String status;
        private List<BidEntry> bidHistory;

        public AuctionDetail() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getStartingPrice() { return startingPrice; }
        public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<BidEntry> getBidHistory() { return bidHistory; }
        public void setBidHistory(List<BidEntry> bidHistory) { this.bidHistory = bidHistory; }

        public Double getCurrentPrice() {
            if (bidHistory != null && !bidHistory.isEmpty()) {
                return bidHistory.getLast().getBidAmount();
            }
            return startingPrice;
        }

        public long getRemainingSeconds() {
            try {
                LocalDateTime end = LocalDateTime.parse(endTime);
                long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), end);
                return Math.max(0, seconds);
            } catch (Exception e) {
                return 0;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BidEntry {
        private String transactionId;
        private BidderInfo bidder;
        private Double bidAmount;
        private String timestamp;

        public BidEntry() {}

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public BidderInfo getBidder() { return bidder; }
        public void setBidder(BidderInfo bidder) { this.bidder = bidder; }
        public Double getBidAmount() { return bidAmount; }
        public void setBidAmount(Double bidAmount) { this.bidAmount = bidAmount; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        @Override
        public String toString() {
            String bidderName = (bidder != null && bidder.getUsername() != null)
                    ? bidder.getUsername() : "Unknown";
            String time = timestamp != null ? timestamp : "";
            if (time.contains("T")) {
                time = time.substring(time.indexOf("T") + 1);
                if (time.contains(".")) time = time.substring(0, time.indexOf("."));
            }
            return time + " — " + bidderName + ": " + String.format("%,.0f VNĐ", bidAmount);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BidderInfo {
        private String id;
        private String username;

        public BidderInfo() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    public static class BidRequest {
        @JsonProperty("amount")
        private Double amount;

        public BidRequest(Double amount) {
            this.amount = amount;
        }

        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
    }
}
