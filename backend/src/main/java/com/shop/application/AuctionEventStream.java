package com.shop.application;

import com.shop.dto.event.AuctionEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuctionEventStream {
    private final AuctionRepository auctionRepository;
    private final Map<String, Sinks.Many<AuctionEvent>> sinks
            = new ConcurrentHashMap<>();

    @PostConstruct
    public void recoverStreams() {
        auctionRepository.getActives()
                .doOnNext(auctionResp -> {
                    newStream(auctionResp.getId());
                })
                .doOnError(Throwable::printStackTrace)
                .subscribe();
    }

    public void newStream(String auctionID) {
        sinks.putIfAbsent(auctionID, Sinks.many().multicast().onBackpressureBuffer());
    }

    public Flux<AuctionEvent> getStream(String auctionID) {
        if (!sinks.containsKey(auctionID)) {
            return Flux.error(new IllegalStateException("auction stream not found"));
        }
        return sinks.get(auctionID).asFlux();
    }

    public void closeStream(String auctionID) {
        sinks.remove(auctionID);
    }

    public void publish(String auctionID, AuctionEvent event) {
        sinks.get(auctionID).tryEmitNext(event);
    }
}
