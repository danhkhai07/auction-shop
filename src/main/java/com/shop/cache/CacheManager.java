package com.shop.cache;

import reactor.core.publisher.Mono;

import java.util.Objects;

public class CacheManager<K, V> {
    // Gia tri mac dinh cho truong hop chi can boc mot in-memory cache don gian.
    private static final long DEFAULT_MANAGER_EXPIRATION_SECONDS = 8 * 3600L;
    private static final long DEFAULT_CLEANUP_INTERVAL_SECONDS = 10 * 60L;

    //Cac gia tri ma cache hien tai su dung
    private final long instanceExpiration;
    private final long cleanUpInterval;
    private final CacheStore<K, V> cacheStore;

    // Constructor tien loi su dung cac gia tri thoi gian mac dinh cua manager.
    public CacheManager(CacheStore<K, V> cacheStore) {
        this(cacheStore, DEFAULT_MANAGER_EXPIRATION_SECONDS, DEFAULT_CLEANUP_INTERVAL_SECONDS);
    }

    public CacheManager(CacheStore<K, V> cacheStore, long instanceExpiration, long cleanUpInterval) {
        if (instanceExpiration <= 0) {
            throw new IllegalArgumentException("instanceExpiration can lon hon 0");
        }
        if (cleanUpInterval <= 0) {
            throw new IllegalArgumentException("cleanUpInterval can lon hon 0");
        }

        this.cacheStore = Objects.requireNonNull(cacheStore, "cacheStore nullError");
        this.instanceExpiration = instanceExpiration;
        this.cleanUpInterval = cleanUpInterval;
    }

    // Luu vao cache voi TTL mac dinh khi caller khong truyen ttl rieng.
    public void put(K key, V value) {
        cacheStore.put(key, value, instanceExpiration);
    }

    public void put(K key, V value, long ttl) {
        if (ttl <= 0) {
            throw new IllegalArgumentException("ttl must be greater than 0");
        }
        cacheStore.put(key, value, ttl);
    }

    public V get(K key) {
        return cacheStore.get(key);
    }

    public void delete(K key) {
        cacheStore.delete(key);
    }

    public boolean contains(K key) {
        return cacheStore.contains(key);
    }

    public long getInstanceExpiration() {
        return instanceExpiration;
    }

    public long getDefaultExpiration() {
        return getInstanceExpiration();
    }

    public long getCleanUpInterval() {
        return cleanUpInterval;
    }

    // InMemoryCacheStore da tu xoa entry het han khi doc, nen tam thoi chua co gi de chay.
    public Mono<Void> run() {
        return Mono.empty();
    }

    // Giu ten method cu de code hien tai khong bi vo trong luc don dep API.
    public Mono<Void> Run() {
        return run();
    }
}
