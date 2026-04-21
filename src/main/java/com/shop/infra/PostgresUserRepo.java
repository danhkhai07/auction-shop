package com.shop.infra;

import com.shop.application.UserRepository;
import com.shop.domain.User;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class PostgresUserRepo implements UserRepository {

    @Override
    public Mono<Boolean> existsByID(String id) {
        return null;
    }

    @Override
    public Mono<User> getByID(String id) {
        return null;
    }

    @Override
    public Mono<User> getByName(String name) {
        return null;
    }

    @Override
    public Mono<Void> saveUser(User user) {
        return null;
    }

    @Override
    public Mono<Void> deleteByID(String id) {
        return null;
    }

    @Override
    public Mono<Void> newUser(User user, String password) {
        return null;
    }

    @Override
    public Mono<Void> changePassword(String id, String password) {
        return null;
    }
}
