package com.shop.application;

import com.shop.domain.Role;
import com.shop.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserCleanupService {
    private final AuctionService auctionService;
    private final ItemService itemService;

    private final UserManager userManager;

    public Mono<Void> cleanupUserData(String id, String deleterID, Set<Role> deleterRoles) {
        return userManager.getUserByID(id)
                .flatMap(user -> {
                    Mono<Void> cancelAuctions = Flux.fromIterable(user.getOwnedAuctions())
                            .flatMap(auction -> {
                                auction.cancelAuction(user);
                                return auctionService.updateAuctionStatus(auction);
                            })
                            .then();

                    Mono<Void> deleteItems = Flux.fromIterable(user.getOwnedItems())
                            .flatMap(item -> itemService.deleteItem(item.getId(), deleterID, deleterRoles))
                            .then();

                    return Mono.when(cancelAuctions, deleteItems);
                });
    }
}
