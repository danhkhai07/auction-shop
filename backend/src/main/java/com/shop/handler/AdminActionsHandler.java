package com.shop.handler;

import com.shop.application.AuctionService;
import com.shop.application.ItemService;
import com.shop.application.UserManager;
import com.shop.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AdminActionsHandler {
    private final UserManager userManager;
    private final AuctionService auctionService;
    private final ItemService itemService;

    public Mono<ServerResponse> elevateUser(ServerRequest request) {
        String userID = request.pathVariable("id");
        String requesterID = request.attribute("userID").map(Object::toString).orElseThrow();
        return userManager.getUserByID(requesterID)
                .filter(requester -> requester.hasRole(Role.ADMIN))
                .switchIfEmpty(Mono.error(new IllegalAccessException("unauthorized")))
                .flatMap(requester -> userManager.elevateUser(userID))
                .then(ServerResponse.status(201).build());
    }

    public Mono<ServerResponse> getAllUsers(ServerRequest request) {
        String requesterID = request.attribute("userID").map(Object::toString).orElseThrow();
        return userManager.getUserByID(requesterID)
                .filter(requester -> requester.hasRole(Role.ADMIN))
                .switchIfEmpty(Mono.error(new IllegalAccessException("unauthorized")))
                .flatMap(requester -> ServerResponse.ok().body(userManager.getAllUsers(), Object.class));
    }

    public Mono<ServerResponse> getAllAuctions(ServerRequest request) {
        String requesterID = request.attribute("userID").map(Object::toString).orElseThrow();
        return userManager.getUserByID(requesterID)
                .filter(requester -> requester.hasRole(Role.ADMIN))
                .switchIfEmpty(Mono.error(new IllegalAccessException("unauthorized")))
                .flatMap(requester -> ServerResponse.ok().body(auctionService.getAllAuctions(), Object.class));
    }

    public Mono<ServerResponse> getAllItems(ServerRequest request) {
        String requesterID = request.attribute("userID").map(Object::toString).orElseThrow();
        return userManager.getUserByID(requesterID)
                .filter(requester -> requester.hasRole(Role.ADMIN))
                .switchIfEmpty(Mono.error(new IllegalAccessException("unauthorized")))
                .flatMap(requester -> ServerResponse.ok().body(itemService.getAllItems(), Object.class));
    }
}
