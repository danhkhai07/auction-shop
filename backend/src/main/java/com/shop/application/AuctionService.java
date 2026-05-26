package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Auction;
import com.shop.domain.AuctionStatus;
import com.shop.domain.Role;
import com.shop.domain.User;
import com.shop.dto.request.UploadAuctionRequest;
import com.shop.dto.response.GetAuctionResponse;
import com.shop.dto.response.GetUserResponse;
import com.shop.dto.response.IDResponse;
import de.huxhorn.sulky.ulid.ULID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final ItemService itemService;
    private final UserManager userManager;
    private final ULID ulid;
    private final CacheManager cacheManager;

    public Mono<Auction> getAuctionByID(String id){
        Mono<Auction> stream;
        if (cacheManager.contains(id)) {
            stream = Mono.just(cacheManager.get(id))
                    .filter(obj -> obj instanceof Auction)
                    .cast(Auction.class);
        } else {
            stream = auctionRepository.getByID(id)
                    .doOnNext(auction -> cacheManager.put(id, auction));
        }

        return stream.switchIfEmpty(Mono.error(new IllegalStateException("auction not found")));
    }

    public Mono<GetAuctionResponse> getAuctionResponseByID(String id){
        return getAuctionByID(id)
                .map(this::toResponse);
    }

    public Mono<List<GetAuctionResponse>> getAllAuctions(){
        return auctionRepository.getAll()
                .map(GetAuctionResponse::new)
                .collectList();
    }

    public Flux<GetAuctionResponse> getActiveAuctions(){
        return auctionRepository.getActives()
                .switchIfEmpty(Mono.error(new IllegalStateException("no auction found")))
                .map(this::toResponse);
    }

    private GetAuctionResponse toResponse(Auction auction) {
        return new GetAuctionResponse(auction);
    }

    public Mono<Void> deleteAuction(String id, String deleterID, Set<Role> deleterRoles){
        // can only delete if user owns the auction or is admin
        if (deleterRoles == null) return Mono.error(new IllegalAccessException("unauthorized"));
        boolean deleterIsAdmin = deleterRoles.contains(Role.ADMIN);
        boolean deleterIsUser = deleterRoles.contains(Role.USER);
        if (!deleterIsUser && !deleterIsAdmin) return Mono.error(new IllegalAccessException("unauthorized"));

        Mono<Auction> stream;
        if (cacheManager.contains(id)) {
            stream = Mono.just((Auction) cacheManager.get(id));
        } else {
            stream = auctionRepository.getByID(id);
        }

        return stream
                .switchIfEmpty(Mono.error(new IllegalStateException("auction does not exist")))
                .filter(auction -> (
                        auction.getItem().getSeller().getId().equals(deleterID) || deleterIsAdmin)
                )
                .switchIfEmpty(Mono.error(new IllegalAccessException("unauthorized")))
                .flatMap(auction -> {
                    if (auction.getStatus() == AuctionStatus.RUNNING) {
                        return Mono.error(new IllegalStateException("running auction must be cancelled before delete"));
                    }
                    if (!auction.getBidHistory().isEmpty()) {
                        return Mono.error(new IllegalStateException("auction with bids cannot be deleted"));
                    }
                    return auctionRepository.deleteByID(id);
                })
                .then(Mono.fromRunnable(() -> cacheManager.delete(id)));
    }

    public Mono<IDResponse> newAuction(String posterID, UploadAuctionRequest request) {
        String id = ulid.nextULID();
        return itemService.getItemByID(request.itemID())
                .switchIfEmpty(Mono.error(new IllegalStateException("item does not exist")))
                .flatMap(item -> auctionRepository.existsByItemID(item.getId())
                        .flatMap(exists -> {
                            if (exists) {
                                return Mono.error(new IllegalStateException("item already has an auction"));
                            }
                            return Mono.just(item);
                        }))
                .flatMap(item -> {
                    if (!posterID.equals(item.getSeller().getId()))
                        return Mono.error(new IllegalAccessException("poster is not auction owner"));
                    Auction auction = new Auction(
                            id,
                            item,
                            request.startingPrice(),
                            request.startTime(),
                            request.endTime()
                    );
                    return userManager.getUserByID(item.getSeller().getId())
                            .flatMap(owner -> auctionRepository.newAuction(auction)
                                    .then(Mono.fromRunnable(() -> {
                                        cacheManager.put(auction.getId(), auction);
                                        owner.addAuction(auction);
                                    }))
                                    .then(userManager.updateUser(owner)));
                })
                .thenReturn(new IDResponse(id));
    }

    public Mono<Void> updateAuctionDetails(String id, String posterID, UploadAuctionRequest request) {
        return itemService.getItemByID(request.itemID())
                .switchIfEmpty(Mono.error(new IllegalStateException("item does not exist")))
                .flatMap(item -> {
                    if (!posterID.equals(item.getSeller().getId()))
                        return Mono.error(new IllegalAccessException("poster is not auction owner"));
                    Auction auction = new Auction(
                            id,
                            item,
                            request.startingPrice(),
                            request.startTime(),
                            request.endTime()
                    );
                    if (cacheManager.contains(id)) {
                        cacheManager.put(id, auction);
                    }
                    return auctionRepository.saveAuction(auction);
                });
    }

    public Mono<Void> updateAuctionStatus(Auction auction) {
        return auctionRepository.saveAuction(auction)
                .then(Mono.fromRunnable(() -> cacheManager.put(auction.getId(), auction)));
    }

    public Mono<Void> finishAuction(Auction auction) {
        return auctionRepository.saveAuction(auction)
                .then(Mono.defer(() -> {
                    if (!auction.hasWinner()) {
                        return Mono.empty();
                    }
                    return itemService.transferItemToUser(auction.getItem(), auction.getCurrentHighestBidder());
                }))
                .then(Mono.fromRunnable(() -> cacheManager.delete(auction.getId())));
    }
}
