package com.shop.application;

import com.shop.domain.Item;

import reactor.core.publisher.Mono;

public interface ItemRepository {

    Mono<Boolean> existsByID(String id);

    Mono<Item> getByID(String id);
    Mono<Item> getByName(String name);

    Mono<Void> saveItem(Item item);
    Mono<Void> deleteByID(String id);
    Mono<Void> newItem(Item item);
}
