package com.shop.domain.cache;

public interface CacheManager {
    // should be threaded
    public int Run();

    // create new key-value
    public int Create();

    // get value from key
    public int Get();

    // delete key-value
    public int Delete();
}
