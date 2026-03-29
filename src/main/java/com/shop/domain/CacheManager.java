package com.shop.domain;

public abstract class CacheManager {
    protected CacheStore cache;
    // Expiration time of a cache item
    protected int defaultExpiration = 8 * 3600; // 8 hours
    // Time between each cleanup: checking whether all cache items are valid
    protected int cleanUpInterval = 10 * 60; // 10 minutes

    CacheManager(CacheStore c, int defaultExpiration, int cleanUpInterval){
        this.cache = c;
        this.defaultExpiration = defaultExpiration;
        this.cleanUpInterval = cleanUpInterval;
    }

    public abstract int Run();
    public abstract int Create();
    public abstract int Get();
    public abstract int Delete();
}
