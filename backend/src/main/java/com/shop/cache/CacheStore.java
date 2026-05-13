package com.shop.cache;

public interface CacheStore<K, V> {
    void put(K key, V value, long ttl);
    V get(K key);
    void delete(K key);
    boolean contains(K key);

    // bruh
    // Xoa cac entry da het han va tra ve so luong entry da duoc don dep.
    int cleanExpiredEntries();
}
