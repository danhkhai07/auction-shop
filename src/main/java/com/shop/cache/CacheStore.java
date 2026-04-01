package com.shop.cache;

public interface CacheStore<K, V> {
    int put(K key, V value);
    V get(K key);
    void delete(K key);
    boolean contains(K key);
}