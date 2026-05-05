package com.shop.handler;

import com.shop.application.AuthService;
import com.shop.application.UserManager;
import com.shop.dto.request.EmptyBodyRequest;
import com.shop.dto.request.LoginRequest;
import com.shop.dto.request.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class    AuthHandler {

    private final AuthService authService;
    private final UserManager userManager;

    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(RegisterRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("missing body")))
                .flatMap(req -> {
                    if (!authService.isValidUsername(req.username())) {
                        return Mono.error(new IllegalArgumentException("invalid username"));
                    }
                    if (!authService.isValidPassword(req.password())) {
                        return Mono.error(new IllegalArgumentException("invalid password"));
                    }
                    return authService.register(req);
                })
                .flatMap(response -> ServerResponse.status(201).bodyValue(response));
    }

    public Mono<ServerResponse> login(ServerRequest request) {
        String invalidCredentialsMessage = "username or password is invalid";
        return request.bodyToMono(LoginRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("missing body")))
                .flatMap(req -> {
                    if (!authService.isValidUsername(req.username())) {
                        return Mono.error(new IllegalArgumentException(invalidCredentialsMessage));
                    }
                    if (!authService.isValidPassword(req.password())) {
                        return Mono.error(new IllegalArgumentException(invalidCredentialsMessage));
                    }
                    return authService.login(req);
                })
                .flatMap(response -> ServerResponse.status(201).bodyValue(response));
    }

    public Mono<ServerResponse> me(ServerRequest request) {
        return request.bodyToMono(EmptyBodyRequest.class)
                .flatMap(req -> userManager.getUserByID(
                        String.valueOf(request.attribute("userID"))))
                .flatMap(response -> ServerResponse.status(200).bodyValue(response));
    }
}
