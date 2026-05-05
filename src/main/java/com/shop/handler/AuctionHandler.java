package com.shop.handler;

import com.shop.application.AuctionService;
import com.shop.application.UserManager;
import com.shop.domain.User;
import com.shop.dto.request.BidRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuctionHandler {
    private final AuctionService auctionService;
    private final UserManager userManager;

    public Mono<ServerResponse> bid(ServerRequest request) {
        String auctionID = request.pathVariable("id");

        Mono<BidRequest> bidRequestMono = request.bodyToMono(BidRequest.class);
        Mono<User> bidderMono = userManager.getUserByID(request.attributes().get("userID").toString());

        return Mono.zip(bidRequestMono, bidderMono)
                .flatMap(tuple -> {
                    BidRequest bidRequest = tuple.getT1();
                    User bidder = tuple.getT2();

                    return auctionService.getAuctionByID(auctionID)
                            .switchIfEmpty(Mono.error(new IllegalStateException("auction does not exist")))
                            .flatMap(auction -> {
                                auction.placeBid(bidder, bidRequest.amount());
                                return auctionService.updateAuctionStatus(auction);
                            });
                })
                .flatMap(v -> ServerResponse.status(201).build());
    }
}
