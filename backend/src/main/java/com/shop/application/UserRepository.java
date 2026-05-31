package com.shop.application;

import com.shop.domain.User;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository {

    Mono<Boolean> existsByID(String id);

    Flux<User> getAll();
    Mono<User> getByID(String id);
    Mono<User> getByName(String name);

    Mono<Void> saveUser(User user);
    Mono<Void> deleteByID(String id);
    Mono<Void> newUser(User user);
    Mono<Void> changePassword(String id, String password);
    Mono<Void> banByID(String id, String reason, String bannedBy);
    Mono<Void> unbanByID(String id);
    Mono<Void> addBalance(String id, java.math.BigDecimal amount);
    Mono<Void> deductBalance(String id, java.math.BigDecimal amount);
}
