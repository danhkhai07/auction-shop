package com.shop.handler;

import com.shop.application.AuctionService;
import com.shop.application.ItemService;
import com.shop.application.UserManager;
import com.shop.domain.Role;
import com.shop.dto.request.EmptyBodyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DeleteHandler {
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final UserManager userManager;

    public Mono<ServerResponse> deleteUser(ServerRequest request) {
        return userManager.deleteUser(
                    request.pathVariable("id"),
                    (String) request.attributes().getOrDefault("userID", ""),
                    (Set<Role>) request.attributes().getOrDefault("resolved_role", null)
                )
                .flatMap(b -> ServerResponse.status(201).bodyValue(new EmptyBodyRequest()))
                .onErrorResume(IllegalStateException.class,
                        e -> ServerResponse
                                .badRequest()
                                .bodyValue(
                                    Map.of("error", e.getMessage())
                                )
                )
                .onErrorResume(IllegalAccessException.class,
                        e -> ServerResponse
                                .status(401)
                                .build()
                );
    }

    public Mono<ServerResponse> deleteItem(ServerRequest request) {
        return itemService.deleteItem(
                        request.pathVariable("id"),
                        (String) request.attributes().getOrDefault("userID", ""),
                        (Set<Role>) request.attributes().getOrDefault("resolved_role", null)
                )
                .flatMap(b -> ServerResponse.status(201).bodyValue(new EmptyBodyRequest()))
                .onErrorResume(IllegalStateException.class,
                        e -> ServerResponse
                                .badRequest()
                                .bodyValue(
                                        Map.of("error", e.getMessage())
                                )
                )
                .onErrorResume(IllegalAccessException.class,
                        e -> ServerResponse
                                .status(401)
                                .build()
                );
    }

    public Mono<ServerResponse> deleteAuction(ServerRequest request) {
        return auctionService.deleteAuction(
                        request.pathVariable("id"),
                        (String) request.attributes().getOrDefault("userID", ""),
                        (Set<Role>) request.attributes().getOrDefault("resolved_role", null)
                )
                .flatMap(b -> ServerResponse.status(201).bodyValue(new EmptyBodyRequest()))
                .onErrorResume(IllegalStateException.class,
                        e -> ServerResponse
                                .badRequest()
                                .bodyValue(
                                        Map.of("error", e.getMessage())
                                )
                )
                .onErrorResume(IllegalAccessException.class,
                        e -> ServerResponse
                                .status(401)
                                .build()
                );
    }
}