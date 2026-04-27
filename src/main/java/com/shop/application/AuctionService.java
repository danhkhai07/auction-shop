package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Auction;
import com.shop.dto.response.GetAuctionResponse;
import com.shop.infra.InMemoryCacheStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
}
