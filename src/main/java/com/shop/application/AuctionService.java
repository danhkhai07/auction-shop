package com.shop.application;

public class AuctionService {
    private final AuctionRepository repo;

    public AuctionService(AuctionRepository repo) {
        this.repo = repo;
    }
}
