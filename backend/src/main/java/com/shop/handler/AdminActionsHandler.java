package com.shop.handler;

import com.shop.application.AuctionEventStream;
import com.shop.application.AuctionService;
import com.shop.application.ItemService;
import com.shop.application.UserManager;
import com.shop.domain.Role;
import com.shop.domain.User;
import com.shop.dto.event.AuctionEvent;
import com.shop.dto.request.BanUserRequest;
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
    private final AuctionEventStream auctionEventStream;

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

    public Mono<ServerResponse> banUser(ServerRequest request) {
        String targetUserId = request.pathVariable("id");
        String requesterID = request.attribute("userID").map(Object::toString).orElseThrow();
        return userManager.getUserByID(requesterID)
                .filter(requester -> requester.hasRole(Role.ADMIN))
                .switchIfEmpty(Mono.error(new IllegalAccessException("unauthorized")))
                .then(request.bodyToMono(BanUserRequest.class))
                .flatMap(body -> userManager.banUser(targetUserId, requesterID, body.reason()))
                .then(ServerResponse.status(204).build());
    }

    public Mono<ServerResponse> unbanUser(ServerRequest request) {
        String targetUserId = request.pathVariable("id");
        String requesterID = request.attribute("userID").map(Object::toString).orElseThrow();
        return userManager.getUserByID(requesterID)
                .filter(requester -> requester.hasRole(Role.ADMIN))
                .switchIfEmpty(Mono.error(new IllegalAccessException("unauthorized")))
                .flatMap(requester -> userManager.unbanUser(targetUserId, requesterID))
                .then(ServerResponse.status(204).build());
    }

    public Mono<ServerResponse> forceCancelAuction(ServerRequest request) {
        String auctionId = request.pathVariable("id");
        String requesterID = request.attribute("userID").map(Object::toString).orElseThrow();

        return userManager.getUserByID(requesterID)
                .filter(requester -> requester.hasRole(Role.ADMIN))
                .switchIfEmpty(Mono.error(new IllegalAccessException("unauthorized")))
                .flatMap(admin -> auctionService.getAuctionByID(auctionId)
                        .switchIfEmpty(Mono.error(new IllegalStateException("auction does not exist")))
                        .flatMap(auction -> {
                            User highestBidder = auction.getCurrentHighestBidder();
                            var lockedAmount = auction.getCurrentHighestPrice();

                            auction.forceCancel();

                            Mono<Void> refundHighestBidder = Mono.empty();
                            if (highestBidder != null
                                    && lockedAmount != null
                                    && lockedAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
                                refundHighestBidder = userManager.getUserByID(highestBidder.getId())
                                        .flatMap(bidder -> {
                                            bidder.addToBalance(lockedAmount);
                                            return userManager.updateUser(bidder);
                                        });
                            }

                            return refundHighestBidder
                                    .then(auctionService.updateAuctionStatus(auction))
                                    .doOnSuccess(v -> {
                                        auctionEventStream.publish(auctionId,
                                                new AuctionEvent("AUCTION_CANCELLED", auction));
                                        auctionEventStream.closeStream(auctionId);
                                    });
                        }))
                .then(ServerResponse.status(201).build());
    }
}
