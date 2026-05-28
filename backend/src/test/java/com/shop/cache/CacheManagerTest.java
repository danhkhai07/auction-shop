package com.shop.cache;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CacheManagerTest {

    @Test
    void getAsReturnsTypedCachedValueWithSingleStoreRead() {
        CountingCacheStore<Object, Object> store = new CountingCacheStore<>();
        CacheManager<Object, Object> cacheManager = new CacheManager<>(store, 60, 30);
        cacheManager.put("id", "cached-value");

        Optional<String> value = cacheManager.getAs("id", String.class);

        assertThat(value).contains("cached-value");
        assertThat(store.getCount()).hasValue(1);
    }

    @Test
    void getAsReturnsEmptyForMissingOrWrongTypeValues() {
        CountingCacheStore<Object, Object> store = new CountingCacheStore<>();
        CacheManager<Object, Object> cacheManager = new CacheManager<>(store, 60, 30);
        cacheManager.put("number", 100);

        assertThat(cacheManager.getAs("missing", String.class)).isEmpty();
        assertThat(cacheManager.getAs("number", String.class)).isEmpty();
    }

    private static final class CountingCacheStore<K, V> implements CacheStore<K, V> {
        private final java.util.Map<K, V> values = new java.util.HashMap<>();
        private final AtomicInteger getCount = new AtomicInteger();

        @Override
        public void put(K key, V value, long ttl) {
            values.put(key, value);
        }

        @Override
        public V get(K key) {
            getCount.incrementAndGet();
            return values.get(key);
        }

        @Override
        public void delete(K key) {
            values.remove(key);
        }

        @Override
        public boolean contains(K key) {
            return values.containsKey(key);
        }

        @Override
        public int cleanExpiredEntries() {
            return 0;
        }

        AtomicInteger getCount() {
            return getCount;
        }
    }
}
