package com.shop.handler;

import com.shop.application.AuctionEventStream;
import com.shop.application.AuctionService;
import com.shop.application.UserManager;
import com.shop.domain.Auction;
import com.shop.domain.User;
import com.shop.dto.event.AuctionEvent;
import com.shop.dto.request.BidRequest;
import com.shop.dto.request.ExtendAuctionTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuctionHandler {
    private final AuctionService auctionService;
    private final UserManager userManager;
    private final AuctionEventStream stream;

    @Transactional
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

                    // Check if user has enough balance
                    if (!bidder.hasEnoughBalance(bidRequest.amount())) {
                        return Mono.error(new IllegalArgumentException("Insufficient balance. Available: " + bidder.getBalance() + ", Required: " + bidRequest.amount()));
                    }

                    return auctionService.getAuctionByIDForUpdate(auctionID)
                            .flatMap(auction -> {
                                User previousBidder = auction.getCurrentHighestBidder();
                                java.math.BigDecimal previousPrice = auction.getCurrentHighestPrice();

                                auction.placeBid(bidder, bidRequest.amount());
                                
                                Mono<Void> updatePreviousBidder = Mono.empty();
                                if (previousBidder != null) {
                                    if (previousBidder.getId().equals(bidder.getId())) {
                                        updatePreviousBidder = userManager.addBalance(bidder.getId(), previousPrice);
                                    } else {
                                        updatePreviousBidder = userManager.addBalance(previousBidder.getId(), previousPrice);
                                    }
                                }

                                Mono<Void> deductNewBid = userManager.deductBalance(bidder.getId(), bidRequest.amount());

                                boolean antiSniped = LocalDateTime.now().isAfter(auction.getEndTime().minusMinutes(1));
                                if (antiSniped) {
                                    auction.extendEndtime(LocalDateTime.now().plusMinutes(1));
                                }
                                
                                return updatePreviousBidder.then(deductNewBid).then(
                                    auctionService.updateAuctionStatus(auction)
                                )
                                .doOnSuccess(v -> {
                                    stream.publish(auctionID,
                                            new AuctionEvent("BID_PLACED", auction));
                                    if (antiSniped) {
                                        stream.publish(auctionID,
                                            new AuctionEvent("ANTI_SNIPE_AUCTION_EXTENDED", auction));
                                    }
                                });
                            });
                })
                .then(ServerResponse.status(201).build());
    }

    @Transactional
    public Mono<ServerResponse> cancelAuction(ServerRequest request) {
        String auctionID = request.pathVariable("id");
        return userManager.getUserByID(request.attributes().get("userID").toString())
                .flatMap(user ->
                        auctionService.getAuctionByIDForUpdate(auctionID)
                                .switchIfEmpty(Mono.error(new IllegalStateException("auction does not exist")))
                                .flatMap(auction -> {
                                    User highestBidder = auction.getCurrentHighestBidder();
                                    java.math.BigDecimal highestPrice = auction.getCurrentHighestPrice();

                                    auction.cancelAuction(user);

                                    Mono<Void> updateHighestBidder = Mono.empty();
                                    if (highestBidder != null) {
                                        updateHighestBidder = userManager.addBalance(highestBidder.getId(), highestPrice);
                                    }

                                    return updateHighestBidder.then(auctionService.updateAuctionStatus(auction))
                                            .doOnSuccess(v -> {
                                                stream.publish(auctionID,
                                                        new AuctionEvent("AUCTION_CANCELLED", auction));
                                                stream.closeStream(auctionID);
                                            });
                                })
                )
                .then(ServerResponse.status(201).build());
    }

    @Transactional
    public Mono<ServerResponse> startAuction(ServerRequest request) {
        String auctionID = request.pathVariable("id");
        return userManager.getUserByID(request.attributes().get("userID").toString())
                .flatMap(user ->
                        auctionService.getAuctionByIDForUpdate(auctionID)
                                .filter(auction -> auction.getItem().getSeller().getId().equals(user.getId()))
                                .switchIfEmpty(Mono.error(new IllegalStateException("unauthorized")))
                                .flatMap(auction -> {
                                    auction.startAuction();
                                    return auctionService.updateAuctionStatus(auction)
                                        .doOnSuccess(v -> {
                                            stream.newStream(auctionID);
                                            stream.publish(auctionID,
                                                    new AuctionEvent("AUCTION_STARTED", auction));
                                        });
                                })
                )
                .then(ServerResponse.status(201).build());
    }

    @Transactional
    public Mono<ServerResponse> pauseAuction(ServerRequest request) {
        String auctionID = request.pathVariable("id");
        return userManager.getUserByID(request.attributes().get("userID").toString())
                .flatMap(user ->
                        auctionService.getAuctionByIDForUpdate(auctionID)
                                .filter(auction -> auction.getItem().getSeller().getId().equals(user.getId()))
                                .switchIfEmpty(Mono.error(new IllegalStateException("unauthorized")))
                                .flatMap(auction -> {
                                    auction.pauseAuction();
                                    return auctionService.updateAuctionStatus(auction)
                                            .doOnSuccess(v -> {
                                                stream.publish(auctionID,
                                                        new AuctionEvent("AUCTION_PAUSED", auction));
                                                stream.closeStream(auctionID);
                                            });
                                })
                )
                .then(ServerResponse.status(201).build());
    }

    @Transactional
    public Mono<ServerResponse> unpauseAuction(ServerRequest request) {
        String auctionID = request.pathVariable("id");
        return userManager.getUserByID(request.attributes().get("userID").toString())
                .flatMap(user ->
                        auctionService.getAuctionByIDForUpdate(auctionID)
                                .filter(auction -> auction.getItem().getSeller().getId().equals(user.getId()))
                                .switchIfEmpty(Mono.error(new IllegalStateException("unauthorized")))
                                .flatMap(auction -> {
                                    auction.unpauseAuction();
                                    return auctionService.updateAuctionStatus(auction)
                                            .doOnSuccess(v -> {
                                                stream.newStream(auctionID);
                                                stream.publish(auctionID,
                                                        new AuctionEvent("AUCTION_UNPAUSED", auction));
                                            });
                                })
                )
                .then(ServerResponse.status(201).build());
    }

    @Transactional
    public Mono<ServerResponse> finishAuction(ServerRequest request) {
        String auctionID = request.pathVariable("id");
        return userManager.getUserByID(request.attributes().get("userID").toString())
                .flatMap(user ->
                    auctionService.getAuctionByIDForUpdate(auctionID)
                        .filter(auction -> auction.getItem().getSeller().getId().equals(user.getId()))
                        .switchIfEmpty(Mono.error(new IllegalStateException("unauthorized")))
                        .flatMap(auction -> {
                            auction.finishAuction();

                            Mono<Void> updateSeller = Mono.empty();
                            if (auction.hasWinner()) {
                                User seller = auction.getItem().getSeller();
                                updateSeller = userManager.addBalance(seller.getId(), auction.getFinalPrice());
                            }

                            return updateSeller.then(auctionService.finishAuction(auction))
                                    .doOnSuccess(v -> {
                                        stream.publish(auctionID,
                                                new AuctionEvent("AUCTION_FINISHED", auction));
                                        stream.closeStream(auctionID);
                                    });
                        })
                )
                .then(ServerResponse.status(201).build());
    }

    @Transactional
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

                    return auctionService.getAuctionByIDForUpdate(auctionID)
                            .filter(auction -> auction.getItem().getSeller().getId().equals(user.getId()))
                            .switchIfEmpty(Mono.error(new IllegalStateException("unauthorized")))
                            .flatMap(auction -> {
                                auction.extendEndtime(extendTimeRequest.newEndTime());
                                return auctionService.updateAuctionStatus(auction)
                                        .doOnSuccess(v -> {
                                            stream.publish(auctionID,
                                                    new AuctionEvent("AUCTION_EXTENDED", auction));
                                        });
                            });
                })
                .then(ServerResponse.status(201).build());
    }

    public Mono<ServerResponse> stream(ServerRequest request) {
        String auctionID = request.pathVariable("id");
        Flux<AuctionEvent> flux = stream.getStream(auctionID)
                .doOnCancel(() -> {
                    System.out.println("client disconnected from auction stream id: " + auctionID);
                });

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(flux, AuctionEvent.class);
    }
}
