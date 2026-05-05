package com.shop.infra;

import com.shop.application.UserRepository;
import com.shop.domain.User;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class PostgresUserRepo implements UserRepository {

    @Override
    public Mono<Boolean> existsByID(String id) {
        return Mono.empty();
    }

    @Override
    public Mono<User> getByID(String id) {
        return Mono.empty();
    }

    @Override
    public Mono<User> getByName(String name) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> saveUser(User user) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> deleteByID(String id) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> newUser(User user) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> changePassword(String id, String password) {
        return Mono.empty();
    }
}
