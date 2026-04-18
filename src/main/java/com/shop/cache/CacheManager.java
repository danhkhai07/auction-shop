package com.shop.cache;

import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class CacheManager<K, V> implements AutoCloseable {
    // Gia tri mac dinh cho truong hop chi can boc mot in-memory cache don gian.
    private static final long DEFAULT_MANAGER_EXPIRATION_SECONDS = 8 * 3600L;
    private static final long DEFAULT_CLEANUP_INTERVAL_SECONDS = 10 * 60L;

    // Cac gia tri ma cache hien tai su dung.
    private final long instanceExpiration;
    private final long cleanUpInterval;
    private final CacheStore<K, V> cacheStore;

    // Khoa de tranh nhieu luong cung start/stop cleanup task mot luc.
    private final Object schedulerLock = new Object();

    // Giu tham chieu toi luong nen chay cleanup dinh ky.
    private volatile ScheduledExecutorService cleanupExecutor;

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

    // Khoi dong cleanup task tren mot thread nen rieng.
    public Mono<Void> run() {
        return Mono.fromRunnable(this::startCleanupTask).then();
    }

    // Giu ten method cu de code hien tai khong bi vo trong luc don dep API.
    public Mono<Void> Run() {
        return run();
    }

    // Tao mot scheduler chi dung 1 luong nen de don dep cache theo chu ky.
    public void startCleanupTask() {
        synchronized (schedulerLock) {
            if (isCleanupTaskRunning()) {
                return;
            }

            cleanupExecutor = Executors.newSingleThreadScheduledExecutor(new CleanupThreadFactory());
            cleanupExecutor.scheduleAtFixedRate(
                    this::cleanExpiredEntriesSafely,
                    cleanUpInterval,
                    cleanUpInterval,
                    TimeUnit.SECONDS
            );
        }
    }

    // Dung luong cleanup khi khong con can manager nay nua.
    public void stopCleanupTask() {
        synchronized (schedulerLock) {
            if (cleanupExecutor == null) {
                return;
            }

            cleanupExecutor.shutdownNow();
            cleanupExecutor = null;
        }
    }

    // Kiem tra cleanup task da duoc khoi dong va chua bi dung hay chua.
    public boolean isCleanupTaskRunning() {
        ScheduledExecutorService currentExecutor = cleanupExecutor;
        return currentExecutor != null && !currentExecutor.isShutdown();
    }

    @Override
    public void close() {
        stopCleanupTask();
    }

    private void cleanExpiredEntriesSafely() {
        try {
            cacheStore.cleanExpiredEntries();
        } catch (RuntimeException exception) {
            // Chan loi tai cleanup thread de task dinh ky khong bi chet dot ngot.
            System.err.println("Cache cleanup bi loi: " + exception.getMessage());
        }
    }

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
