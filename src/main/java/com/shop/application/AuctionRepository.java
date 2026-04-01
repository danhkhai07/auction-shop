package com.shop.application;

import com.shop.domain.Auction;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface AuctionRepository {
    Mono<Auction> getByID(String id);
    // get all active auctions
    Flux<Auction> getActive();
    Mono<Void> save(Auction auction);
    Mono<Void> delete(int id);
}
