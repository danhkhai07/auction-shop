package com.frontendauction.model;

public record BidResult(boolean success, String errorMessage) {
    public static BidResult ok() {
        return new BidResult(true, null);
    }
    public static BidResult failure(String errorMessage) {
        return new BidResult(false, errorMessage);
    }
}
