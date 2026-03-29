package com.shop.domain.cache;

public interface CacheStore<T, K> {
    public abstract int Create();
    public abstract int Get();
    public abstract int Delete();
}
