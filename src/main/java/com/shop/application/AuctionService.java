package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Auction;
import com.shop.domain.Item;
import com.shop.domain.Role;
import com.shop.dto.response.GetAuctionResponse;
import com.shop.infra.InMemoryCacheStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final CacheManager IDCache = new CacheManager(
            new InMemoryCacheStore<String, Auction>(),
            3 * 60,
            10 * 60
    );

    public Mono<GetAuctionResponse> getAuctionByID(String id){
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

        return stream
                .switchIfEmpty(Mono.error(new IllegalAccessException("auction not found")))
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
}
