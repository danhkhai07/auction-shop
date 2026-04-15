package com.shop.infra;

import com.shop.cache.CacheStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCacheStore<K, V> implements CacheStore<K, V> {
    //Khoi tao Wrapper Class CacheEntry luu tru don vi du lieu cua Cache
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
        if(entry == null)
            return null;
        //Kiem tra xem du lieu da qua han chua
        if(System.currentTimeMillis() > entry.expiredAt) {
            cache.remove(key);
            return null;
        }

        return entry.value;
    }

    @Override
    public void delete(K key) {
        //Xoa du lieu khoi cache
        cache.remove(key);
    }

    @Override
    public boolean contains(K key) {
        //Kiem tra xem trong cache co con du lieu nay khong
        if(get(key)!= null)
            return true;
        return false;
    }

    @Override
    public void put(K key, V value, long ttl) {
        //Tinh thoi gian chinh xac thoi diem du lieu het han
        long expiredAt = System.currentTimeMillis() + ttl * 1000;
        cache.put(key, new CacheEntry<>(value, expiredAt));
    }
}
