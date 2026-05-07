package com.shop.handler;

import com.shop.application.AuctionService;
import com.shop.application.UserManager;
import com.shop.domain.Auction;
import com.shop.domain.User;
import com.shop.dto.request.BidRequest;
import com.shop.dto.request.ExtendAuctionTime;
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

    public Mono<ServerResponse> placeBid(ServerRequest request) {
        String auctionID = request.pathVariable("id");

        Mono<BidRequest> bidRequestMono = request.bodyToMono(BidRequest.class)
                .filter(req -> !req.hasEmptyFields())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("missing fields")));
        Mono<User> bidderMono = userManager.getUserByID(request.attributes().get("userID").toString());

        return Mono.zip(bidRequestMono, bidderMono)
                .flatMap(tuple -> {
                    BidRequest bidRequest = tuple.getT1();
                    User bidder = tuple.getT2();

                    return auctionService.getAuctionByID(auctionID)
                            .flatMap(auction -> {
                                auction.placeBid(bidder, bidRequest.amount());
                                return auctionService.updateAuctionStatus(auction);
                            });
                })
                .flatMap(v -> ServerResponse.status(201).build());
    }

    public Mono<ServerResponse> cancelAuction(ServerRequest request) {
        String auctionID = request.pathVariable("id");
        return userManager.getUserByID(request.attributes().get("userID").toString())
                .flatMap(user ->
                        auctionService.getAuctionByID(auctionID)
                                .switchIfEmpty(Mono.error(new IllegalStateException("auction does not exist")))
                                .flatMap(auction -> {
                                    auction.cancelAuction(user);
                                    return auctionService.updateAuctionStatus(auction);
                                })
                )
                .flatMap(v -> ServerResponse.status(201).build());
    }

    public Mono<ServerResponse> startAuction(ServerRequest request) {
        String auctionID = request.pathVariable("id");
        return userManager.getUserByID(request.attributes().get("userID").toString())
                .flatMap(user ->
                        auctionService.getAuctionByID(auctionID)
                                .filter(auction -> auction.getItem().getSeller().getId().equals(user.getId()))
                                .switchIfEmpty(Mono.error(new IllegalStateException("unauthorized")))
                                .flatMap(auction -> {
                                    auction.startAuction();
                                    return auctionService.updateAuctionStatus(auction);
                                })
                )
                .flatMap(v -> ServerResponse.status(201).build());
    }

    public Mono<ServerResponse> pauseAuction(ServerRequest request) {
        String auctionID = request.pathVariable("id");
        return userManager.getUserByID(request.attributes().get("userID").toString())
                .flatMap(user ->
                        auctionService.getAuctionByID(auctionID)
                                .filter(auction -> auction.getItem().getSeller().getId().equals(user.getId()))
                                .switchIfEmpty(Mono.error(new IllegalStateException("unauthorized")))
                                .flatMap(auction -> {
                                    auction.pauseAuction();
                                    return auctionService.updateAuctionStatus(auction);
                                })
                )
                .flatMap(v -> ServerResponse.status(201).build());
    }

    public Mono<ServerResponse> unpauseAuction(ServerRequest request) {
        String auctionID = request.pathVariable("id");
        return userManager.getUserByID(request.attributes().get("userID").toString())
                .flatMap(user ->
                        auctionService.getAuctionByID(auctionID)
                                .filter(auction -> auction.getItem().getSeller().getId().equals(user.getId()))
                                .switchIfEmpty(Mono.error(new IllegalStateException("unauthorized")))
                                .flatMap(auction -> {
                                    auction.unpauseAuction();
                                    return auctionService.updateAuctionStatus(auction);
                                })
                )
                .flatMap(v -> ServerResponse.status(201).build());
    }

    public Mono<ServerResponse> finishAuction(ServerRequest request) {
        String auctionID = request.pathVariable("id");
        return userManager.getUserByID(request.attributes().get("userID").toString())
                .flatMap(user ->
                    auctionService.getAuctionByID(auctionID)
                        .filter(auction -> auction.getItem().getSeller().getId().equals(user.getId()))
                        .switchIfEmpty(Mono.error(new IllegalStateException("unauthorized")))
                        .flatMap(auction -> {
                            auction.finishAuction();
                            return auctionService.updateAuctionStatus(auction);
                        })
                )
                .flatMap(v -> ServerResponse.status(201).build());
    }

    public Mono<ServerResponse> extendEndtime(ServerRequest request) {
        String auctionID = request.pathVariable("id");

        Mono<ExtendAuctionTime> extendTimeRequestMono = request.bodyToMono(ExtendAuctionTime.class)
                .filter(req -> !req.hasEmptyFields())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("missing fields")));
        Mono<User> userMono = userManager.getUserByID(request.attributes().get("userID").toString());

        return Mono.zip(extendTimeRequestMono, userMono)
                .flatMap(tuple -> {
                    ExtendAuctionTime extendTimeRequest = tuple.getT1();
                    User user = tuple.getT2();

                    return auctionService.getAuctionByID(auctionID)
                            .filter(auction -> auction.getItem().getSeller().getId().equals(user.getId()))
                            .switchIfEmpty(Mono.error(new IllegalStateException("unauthorized")))
                            .flatMap(auction -> {
                                auction.extendEndtime(extendTimeRequest.newEndTime());
                                return auctionService.updateAuctionStatus(auction);
                            });
                })
                .flatMap(v -> ServerResponse.status(201).build());
    }
}
