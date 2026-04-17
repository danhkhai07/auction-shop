package com.shop.application;

import com.shop.domain.User;
import com.shop.dto.request.RegisterRequest;
import com.shop.dto.response.RegisterResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserManager {
    private final UserRepository repo;

    public UserManager(UserRepository repo) {
        this.repo = repo;
    }

    public Mono<RegisterResponse> register(RegisterRequest request) {
        User user = new User()
        repo.newUser()
    }
}
