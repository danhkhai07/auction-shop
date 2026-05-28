package com.frontendauction.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model for SSE events received from /auction/{id}/events endpoint.
 * Maps to backend's AuctionEvent record.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuctionEventData {
    private String type;
    private String status;
    private Double currentHighestPrice;
    private BidderInfo currentHighestBidder;
    private Double finalPrice;
    private String endTime;

    public AuctionEventData() {
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getCurrentHighestPrice() { return currentHighestPrice; }
    public void setCurrentHighestPrice(Double currentHighestPrice) { this.currentHighestPrice = currentHighestPrice; }

    public BidderInfo getCurrentHighestBidder() { return currentHighestBidder; }
    public void setCurrentHighestBidder(BidderInfo currentHighestBidder) { this.currentHighestBidder = currentHighestBidder; }

    public Double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(Double finalPrice) { this.finalPrice = finalPrice; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BidderInfo {
        private String id;
        private String username;

        public BidderInfo() {
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
}
