package com.shop.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class IndexHandler {

    public Mono<ServerResponse> index(ServerRequest request) {
        return ServerResponse.ok().bodyValue(
                Map.of("message", "Welcome to auction-shop API!")
        );
    }
}
