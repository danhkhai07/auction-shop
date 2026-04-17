package com.shop.handler;

import com.shop.application.AuthService;
import com.shop.application.UserManager;
import com.shop.dto.request.RegisterRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class AuthHandler {

    private final AuthService authService;
    private final UserManager userManager;

    public AuthHandler(AuthService authService, UserManager userManager) {
        this.authService = authService;
        this.userManager = userManager;
    }

    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(RegisterRequest.class)
                .switchIfEmpty(Mono.error(new IllegalAccessException("missing body")))
                .flatMap(req -> {
                    if (!authService.isValidUsername(req.username)) {
                        return Mono.error(new IllegalAccessException("invalid username"));
                    }
                    if (!authService.isValidPassword(req.password)) {
                        return Mono.error(new IllegalAccessException("invalid password"));
                    }
                    return userManager.register(req);
                })
                .flatMap(response -> ServerResponse.status(201).bodyValue(response))
                .onErrorResume(IllegalAccessException.class,
                        e -> ServerResponse.
                                badRequest()
                                .bodyValue(
                                        Map.of("error", e.getMessage())
                                ));
    }
}
