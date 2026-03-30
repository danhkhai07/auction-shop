package com.shop.cache;

import reactor.core.publisher.Mono;

public class CacheManager {
    private int defaultExpiration = 8 * 3600;
    private int cleanUpInterval = 10 * 60;
    private CacheStore<?, ?> c;

    // should be threaded
    public Mono<Void> Run(){
    }
}
