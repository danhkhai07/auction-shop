package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Auction;
import com.shop.domain.Role;
import com.shop.dto.request.UploadAuctionRequest;
import com.shop.dto.response.GetAuctionResponse;
import com.shop.dto.response.IDResponse;
import com.shop.infra.InMemoryCacheStore;
import de.huxhorn.sulky.ulid.ULID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final ItemService itemService;
    private final ULID ulid;
    private final CacheManager IDCache = new CacheManager(
            new InMemoryCacheStore<String, Auction>(),
            3 * 60,
            10 * 60
    );

    public Mono<Auction> getAuctionByID(String id){
        Mono<Auction> stream;
        if (IDCache.contains(id)) {
            stream = Mono.just(IDCache.get(id))
                    .filter(obj -> obj instanceof Auction)
                    .cast(Auction.class);
        } else {
            stream = auctionRepository.getByID(id)
                    .filter(auction -> {
                        IDCache.put(id, auction);
                        return true;
                    });
        }

        return stream.switchIfEmpty(Mono.error(new IllegalStateException("auction not found")));
    }

    public Mono<GetAuctionResponse> getAuctionResponseByID(String id){
        return getAuctionByID(id)
                .switchIfEmpty(Mono.error(new IllegalStateException("auction not found")))
                .map(auction -> {
                    GetAuctionResponse response = new GetAuctionResponse(
                            auction.getId(),
                            "Auction",
                            auction.getStartingPrice(),
                            auction.getStartTime(),
                            auction.getEndTime(),
                            auction.getStatus(),
                            auction.getBidHistory()
                    );
                    return response;
                });
    }

    public Flux<GetAuctionResponse> getActiveAuctions(){
        return auctionRepository.getActives()
                .switchIfEmpty(Mono.error(new IllegalStateException("no auction found")))
                .map(auction -> {
                    GetAuctionResponse response = new GetAuctionResponse(
                            auction.getId(),
                            "Auction",
                            auction.getStartingPrice(),
                            auction.getStartTime(),
                            auction.getEndTime(),
                            auction.getStatus(),
                            auction.getBidHistory()
                    );
                    return response;
                });
    }

    public Mono<Void> deleteAuction(String id, String deleterID, Set<Role> deleterRoles){
        // can only delete if user owns the auction or is admin
        if (deleterRoles == null) return Mono.error(new IllegalAccessException("unauthorized"));
        boolean deleterIsAdmin = deleterRoles.contains(Role.ADMIN);
        boolean deleterIsUser = deleterRoles.contains(Role.USER);
        if (!deleterIsUser && !deleterIsAdmin) return Mono.error(new IllegalAccessException("unauthorized"));

        Mono<Auction> stream;
        if (IDCache.contains(id)) {
            stream = Mono.just((Auction) IDCache.get(id));
        } else {
            stream = auctionRepository.existsByID(id)
                    .filter(b -> b)
                    .flatMap(b -> auctionRepository.getByID(id));
        }

        return stream
                .switchIfEmpty(Mono.error(new IllegalStateException("item does not exist")))
                .filter(auction -> (
                        auction.getItem().getSeller().getId().equals(deleterID) || deleterIsAdmin)
                )
                .switchIfEmpty(Mono.error(new IllegalAccessException("unauthorized")))
                .flatMap(b -> auctionRepository.deleteByID(id))
                .filter(v -> {
                    IDCache.delete(id);
                    return true;
                });
    }

    public Mono<IDResponse> newAuction(String posterID, UploadAuctionRequest request) {
        String id = ulid.nextULID();
        return itemService.getItemByID(request.itemID())
                .switchIfEmpty(Mono.error(new IllegalStateException("item does not exist")))
                .flatMap(item -> {
                    if (!posterID.equals(item.getSeller().id))
                        return Mono.error(new IllegalAccessException("poster is not auction owner"));
                    Auction auction = new Auction(
                            id,
                            item,
                            request.startingPrice(),
                            request.startTime(),
                            request.endTime()
                    );
                    return auctionRepository.newAuction(auction);
                })
                .thenReturn(new IDResponse(id));
    }

    public Mono<Void> updateAuction(String id, String posterID, UploadAuctionRequest request) {
        return auctionRepository.existsByID(id)
                .flatMap(exists -> {
                    if (!exists) return Mono.error(new IllegalStateException("item does not exists"));
                    return itemService.getItemByID(request.itemID());
                })
                .switchIfEmpty(Mono.error(new IllegalStateException("item does not exist")))
                .flatMap(item -> {
                    if (!posterID.equals(item.getSeller().id))
                        return Mono.error(new IllegalAccessException("poster is not auction owner"));
                    Auction auction = new Auction(
                            id,
                            item,
                            request.startingPrice(),
                            request.startTime(),
                            request.endTime()
                    );
                    return auctionRepository.saveAuction(auction);
                });
    }
}
