package com.shop.application;

import com.shop.domain.User;
import com.shop.dto.request.RegisterRequest;
import com.shop.dto.response.RegisterResponse;
import com.shop.security.Hash;
import de.huxhorn.sulky.ulid.ULID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class UserManager {
    private final UserRepository userRepository;
    private final ULID ulid;

    public Mono<RegisterResponse> register(RegisterRequest request) {
        User user = new User(ulid.nextULID(), request.username);
        String passwordHash = Hash.hash(request.password);
        return userRepository.getIDByUsername(request.username)
                .flatMap(id ->
                        Mono.<RegisterResponse>error(new IllegalStateException("username already exists")))
                .switchIfEmpty(
                        userRepository.newUser(user, passwordHash)
                                .thenReturn(new RegisterResponse("User created"))
                );
    }
}
