package com.shop.handler;

import com.shop.application.AuctionService;
import com.shop.application.ItemService;
import com.shop.application.UserManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ViewHandler {
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final UserManager userManager;

    public Mono<ServerResponse> getUser(ServerRequest request) {
        String id = request.pathVariable("id");
        return userManager.getUserResponseByID(id)
                .flatMap(response -> ServerResponse.status(200).bodyValue(response));
    }

    public Mono<ServerResponse> getItem(ServerRequest request) {
        String id = request.pathVariable("id");
        return itemService.getItemResponseByID(id)
                .flatMap(response -> ServerResponse.status(200).bodyValue(response));
    }

    public Mono<ServerResponse> getAuction(ServerRequest request) {
        String id = request.pathVariable("id");
        return auctionService.getAuctionResponseByID(id)
                .flatMap(response -> ServerResponse.status(200).bodyValue(response));
    }

    public Mono<ServerResponse> getFeed(ServerRequest request) {
        return auctionService.getActiveAuctions()
                .collectList()
                .flatMap(list -> ServerResponse.status(200).bodyValue(list));
    }
}
