package com.shop.application;

import com.shop.dto.event.AuctionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuctionEventStream {
    private final AuctionService auctionService;
    private Map<String, Sinks.Many<AuctionEvent>> sinks
            = new ConcurrentHashMap<>();

    public void newStream(String auctionID) {
        sinks.put(auctionID, Sinks.many().multicast().onBackpressureBuffer());
    }

    public Flux<AuctionEvent> getStream(String auctionID) {
        if (!sinks.containsKey(auctionID)) {
            return Flux.empty();
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
