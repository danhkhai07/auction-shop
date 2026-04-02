package com.shop.infra;

import com.shop.cache.CacheStore;

public class InMemoryCacheStore<K, V> implements CacheStore<K, V> {

    @Override
    public int put(Object key, Object value) {
        return 0;
    }

    @Override
    public Object get(Object key) {
        return null;
    }

    @Override
    public void delete(Object key) {

    }

    @Override
    public boolean contains(Object key) {
        return false;
    }
}
