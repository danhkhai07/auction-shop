package com.shop.handler;

import com.shop.application.AuctionService;
import com.shop.application.ItemService;
import com.shop.application.UserCleanupService;
import com.shop.application.UserManager;
import com.shop.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DeleteHandler {
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final UserManager userManager;
    private final UserCleanupService userCleanupService;

    public Mono<ServerResponse> deleteUser(ServerRequest request) {
        String id = request.pathVariable("id");
        String requesterId = request.attribute("userID")
                .map(Object::toString)
                .orElseThrow(() -> new IllegalStateException("Missing userID"));
        Set<Role> roles = (Set<Role>) request.attribute("resolved_role")
                .orElseThrow(() -> new IllegalStateException("Missing roles"));
        return userManager.deleteUser(id, requesterId, roles)
                .flatMap(v -> userCleanupService.cleanupUserData(id, requesterId, roles))
                .then(ServerResponse.status(204).build());
    }

    public Mono<ServerResponse> deleteItem(ServerRequest request) {
        String id = request.pathVariable("id");
        String requesterId = request.attribute("userID")
                .map(Object::toString)
                .orElseThrow(() -> new IllegalStateException("Missing userID"));
        Set<Role> roles = (Set<Role>) request.attribute("resolved_role")
                .orElseThrow(() -> new IllegalStateException("Missing roles"));
        return itemService.deleteItem(id, requesterId, roles)
                .then(ServerResponse.status(204).build());
    }

    public Mono<ServerResponse> deleteAuction(ServerRequest request) {
        String id = request.pathVariable("id");
        String requesterId = request.attribute("userID")
                .map(Object::toString)
                .orElseThrow(() -> new IllegalStateException("Missing userID"));
        Set<Role> roles = (Set<Role>) request.attribute("resolved_role")
                .orElseThrow(() -> new IllegalStateException("Missing roles"));
        return auctionService.deleteAuction(id, requesterId, roles)
                .then(ServerResponse.status(204).build());
    }
}