package com.shop.application;

import com.shop.domain.Auction;

import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Locale;

@Repository
public interface AuctionRepository {
    Mono<Auction> getByID(String id);
    // get all active auctions
    Flux<Auction> getActives();

    Mono<Boolean> existsByID(String id);

    Mono<Void> saveAuction(Auction auction);
    Mono<Void> deleteByID(String id);
    Mono<Void> newAuction(Auction auction);

}
