package com.shop.cache;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;

import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Component
public class CacheManager<K, V> {
    // Gia tri mac dinh cho truong hop chi can boc mot in-memory cache don gian.
    private static final long DEFAULT_MANAGER_EXPIRATION_SECONDS = 15 * 60L;
    private static final long DEFAULT_CLEANUP_INTERVAL_SECONDS = 5 * 60L;

    // Cac gia tri ma cache hien tai su dung.
    private final CacheStore<K, V> cacheStore;
    private final long defaultExpirationSeconds;
    private final long cleanupIntervalSeconds;

    // Khoa de tranh nhieu luong cung start/stop cleanup task mot luc.
    private final Object schedulerLock = new Object();

    // Giu tham chieu toi luong nen chay cleanup dinh ky.
    private volatile ScheduledExecutorService cleanupExecutor;

    public CacheManager(
            CacheStore<K, V> cacheStore,
            @Value("${cache.default-expiration-seconds:900}") long defaultExpirationSeconds,
            @Value("${cache.cleanup-interval-seconds:300}") long cleanupIntervalSeconds
    ) {
        this.cacheStore = Objects.requireNonNull(cacheStore, "cacheStore nullError");
        this.defaultExpirationSeconds = defaultExpirationSeconds > 0
                ? defaultExpirationSeconds
                : DEFAULT_MANAGER_EXPIRATION_SECONDS;
        this.cleanupIntervalSeconds = cleanupIntervalSeconds > 0
                ? cleanupIntervalSeconds
                : DEFAULT_CLEANUP_INTERVAL_SECONDS;
    }

    // Luu vao cache voi TTL mac dinh khi caller khong truyen ttl rieng.
    public void put(K key, V value) {
        cacheStore.put(key, value, defaultExpirationSeconds);
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

    public <T> Optional<T> getAs(K key, Class<T> type) {
        Object value = cacheStore.get(key);
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    public void delete(K key) {
        cacheStore.delete(key);
    }

    public boolean contains(K key) {
        return cacheStore.contains(key);
    }

    public long getInstanceExpiration() {
        return defaultExpirationSeconds;
    }

    //Dam bao an toan cho Scheduler
    private void cleanExpiredEntriesSafely() {
        try {
            cacheStore.cleanExpiredEntries();
        } catch (RuntimeException exception) {
            System.err.println("Cache cleanup failed: " + exception.getMessage());
        }
    }

    @PostConstruct// Tao mot scheduler chi dung 1 luong nen de don dep cache theo chu ky.
    private void startCleanupTask() {
        synchronized (schedulerLock) {
            if (isCleanupTaskRunning()) {
                return;
            }

            cleanupExecutor = Executors.newSingleThreadScheduledExecutor(new CleanupThreadFactory());
            cleanupExecutor.scheduleAtFixedRate(
                    this::cleanExpiredEntriesSafely,
                    cleanupIntervalSeconds,
                    cleanupIntervalSeconds,
                    TimeUnit.SECONDS
            );
        }
    }
    // Dung luong cleanup khi khong con can manager nay nua.
    @PreDestroy
    private void stopCleanupTask() {
        synchronized (schedulerLock) {
            if (cleanupExecutor == null) {
                return;
            }

            cleanupExecutor.shutdownNow();
            cleanupExecutor = null;
        }
    }

    // Kiem tra cleanup task da duoc khoi dong va bi dung hay chua.
    public boolean isCleanupTaskRunning() {
        ScheduledExecutorService currentExecutor = cleanupExecutor;
        return currentExecutor != null && !currentExecutor.isShutdown();
    }
    //Tao Factory quan ly viec tao Thread rieng cho viec don dep cache
    private static class CleanupThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            // Dat ten va danh dau daemon de thread nen khong can chan viec tat app.
            Thread thread = new Thread(runnable, "cache-manager-cleanup");
            thread.setDaemon(true);
            return thread;
        }
    }
}
