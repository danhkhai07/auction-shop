package com.shop.domain;

import java.util.Map;
import java.util.HashMap;

public abstract class CacheStore<T, K> {
    Map<T, K> store;

    CacheStore(){
        this.store = new HashMap<>();
    }

    public abstract int Create();
    public abstract int Get();
    public abstract int Delete();
}
