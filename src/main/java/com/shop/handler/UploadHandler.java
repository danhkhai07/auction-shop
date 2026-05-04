package com.shop.handler;

import com.shop.application.AuctionService;
import com.shop.application.ItemService;
import com.shop.dto.request.UploadAuctionRequest;
import com.shop.dto.request.UploadItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UploadHandler {
    private final ItemService itemService;
    private final AuctionService auctionService;

    public Mono<ServerResponse> uploadItem(ServerRequest request) {
        return request.bodyToMono(UploadItemRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("missing body")))
                .flatMap(req -> itemService.newItem(
                        (String) request.attributes().get("userID"),
                        req
                    )
                )
                .flatMap(response -> ServerResponse.status(201).bodyValue(response));
    }

    public Mono<ServerResponse> uploadAuction(ServerRequest request) {
        return request.bodyToMono(UploadAuctionRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("missing body")))
                .flatMap(req -> auctionService.newAuction(
                        (String) request.attributes().get("userID"),
                        req
                    )
                )
                .flatMap(response -> ServerResponse.status(201).bodyValue(response));
    }

    public Mono<ServerResponse> updateItem(ServerRequest request) {
        return request.bodyToMono(UploadItemRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("missing body")))
                .flatMap(req -> itemService.updateItem(
                        request.pathVariable("id"),
                        (String) request.attributes().get("userID"),
                        req
                    )
                )
                .flatMap(v -> ServerResponse.status(201).build());
    }

    public Mono<ServerResponse> updateAuction(ServerRequest request) {
        return request.bodyToMono(UploadAuctionRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("missing body")))
                .flatMap(req -> auctionService.updateAuction(
                                request.pathVariable("id"),
                                (String) request.attributes().get("userID"),
                                req
                        )
                )
                .flatMap(v -> ServerResponse.status(201).build());
    }
}
