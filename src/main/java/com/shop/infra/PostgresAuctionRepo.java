package com.shop.infra;

import com.shop.application.AuctionRepository;
import com.shop.domain.Auction;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class PostgresAuctionRepo implements AuctionRepository {

    @Override
    public Mono<Auction> getByID(String id) {
        return Mono.empty();
    }

    @Override
    public Flux<Auction> getActives() {
        return Flux.empty();
    }

    @Override
    public Mono<Void> saveAuction(Auction auction) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> deleteByID(String id) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> newAuction(Auction auction) {
        return Mono.empty();
    }
}
