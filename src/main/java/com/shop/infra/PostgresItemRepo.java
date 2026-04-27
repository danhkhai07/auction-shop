package com.shop.infra;

import com.shop.application.ItemRepository;
import com.shop.domain.Item;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class PostgresItemRepo implements ItemRepository {
    @Override
    public Mono<Boolean> existsByID(String id) {
        return Mono.empty();
    }

    @Override
    public Mono<Item> getByID(String id) {
        return Mono.empty();
    }

    @Override
    public Mono<Item> getByName(String name) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> saveItem(Item item) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> deleteByID(String id) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> newItem(Item item) {
        return Mono.empty();
    }
}
