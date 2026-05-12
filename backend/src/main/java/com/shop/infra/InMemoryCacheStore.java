package com.shop.infra;

import com.shop.cache.CacheStore;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCacheStore<K, V> implements CacheStore<K, V> {
    // Khoi tao wrapper luu gia tri va thoi diem het han cua tung entry.
    private static class CacheEntry<V>{
        private final V value;
        private final long expiredAt;
        private CacheEntry(V value, long expiredAt){
            this.value = value;
            this.expiredAt = expiredAt;
        }
    }

    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();

    @Override
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        // Kiem tra xem du lieu da qua han chua.
        if (isExpired(entry, System.currentTimeMillis())) {
            // Chi xoa dung entry vua doc ra de tranh xoa nham du lieu moi hon.
            cache.remove(key, entry);
            return null;
        }

        return entry.value;
    }

    @Override
    public void delete(K key) {
        // Xoa du lieu khoi cache.
        cache.remove(key);
    }

    @Override
    public boolean contains(K key) {
        // Kiem tra xem trong cache co con du lieu nay khong.
        return get(key) != null;
    }

    @Override
    public void put(K key, V value, long ttl) {
        if (ttl <= 0) {
            throw new IllegalArgumentException("ttl can lon hon 0");
        }

        // Tinh thoi diem chinh xac ma du lieu het han.
        long expiredAt = System.currentTimeMillis() + ttl * 1000L;
        cache.put(key, new CacheEntry<>(value, expiredAt));
    }

    @Override
    public int cleanExpiredEntries() {
        int removedEntries = 0;
        long currentTime = System.currentTimeMillis();

        // Quet toan bo cache va xoa tung entry het han mot cach an toan voi da luong.
        for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
            if (isExpired(entry.getValue(), currentTime) && cache.remove(entry.getKey(), entry.getValue())) {
                removedEntries++;
            }
        }

        return removedEntries;
    }

    private boolean isExpired(CacheEntry<V> entry, long currentTime) {
        return currentTime > entry.expiredAt;
    }
}
