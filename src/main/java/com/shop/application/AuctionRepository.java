package com.shop.application;

import com.shop.domain.Auction;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface AuctionRepository {
    Mono<Auction> getByID(String id);
    // get all active auctions
    Flux<Auction> getActives();

    Mono<Void> saveAuction(Auction auction);
    Mono<Void> deleteByID(String id);
    Mono<Void> newAuction(Auction auction);
}
