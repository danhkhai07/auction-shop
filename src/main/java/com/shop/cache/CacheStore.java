package com.shop.cache;

public interface CacheStore<K, V> {
    void put(K key, V value, long TTl);
    V get(K key);
    void delete(K key);
    boolean contains(K key);
}