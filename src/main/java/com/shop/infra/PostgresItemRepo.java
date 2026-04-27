package com.shop.infra;

import com.shop.application.ItemRepository;
import com.shop.domain.Item;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class PostgresItemRepo implements ItemRepository {
    @Override
    public Mono<Boolean> existsByID(String id) {
        return null;
    }

    @Override
    public Mono<Item> getByID(String id) {
        return null;
    }

    @Override
    public Mono<Item> getByName(String name) {
        return null;
    }

    @Override
    public Mono<Void> saveItem(Item item) {
        return null;
    }

    @Override
    public Mono<Void> deleteByID(String id) {
        return null;
    }

    @Override
    public Mono<Void> newItem(Item item) {
        return null;
    }
}
