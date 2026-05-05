package com.shop.application;

import com.shop.domain.User;

import reactor.core.publisher.Mono;

public interface UserRepository {

    Mono<Boolean> existsByID(String id);

    Mono<User> getByID(String id);
    Mono<User> getByName(String name);

    Mono<Void> saveUser(User user);
    Mono<Void> deleteByID(String id);
    Mono<Void> newUser(User user);
    Mono<Void> changePassword(String id, String password);
}
