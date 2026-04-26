package com.shop.filter;

import com.shop.security.jwt.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {
    private final JWTService jwtService;

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        String auth = request.headers().firstHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ServerResponse.status(401).build();
        }

        String token = auth.substring(7);
        try {
            String userID = jwtService.extractUserId(token);
            ServerRequest newRequest = ServerRequest.from(request)
                    .attribute("userID", userID)
                    .build();
            return next.handle(newRequest);
        } catch (Exception e) {
            return ServerResponse.status(401).build();
        }
    }
}
