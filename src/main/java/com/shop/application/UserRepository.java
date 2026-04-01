package com.shop.application;

import com.shop.domain.User;

import reactor.core.publisher.Mono;

public interface UserRepository {

    Mono<Boolean> existsByID(String id);

    Mono<User> getByID(String id);
    Mono<Integer> getIDByUsername(String username);

    Mono<Void> setByID(String id, User user);
    Mono<Void> deleteByID(String id);
}
