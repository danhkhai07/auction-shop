package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.cache.CacheStore;
import com.shop.domain.User;
import com.shop.infra.InMemoryCacheStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserManager {
    private final UserRepository userRepository;
    private final CacheManager IDCache = new CacheManager(
            new InMemoryCacheStore<String, User>(),
            3 * 60,
            10 * 60
    );
    private final CacheManager NameCache = new CacheManager(
            new InMemoryCacheStore<String, User>(),
            3 * 60,
            10 * 60
    );

    public Mono<User> getUserByID(String id){
        return userRepository.getByID(id)
                .filter(user -> {
                    IDCache.put(id, user);
                    return false;
                });
    }

    public Mono<User> getUserByName(String name){
        return userRepository.getByName(name)
                .filter(user -> {
                    NameCache.put(name, user);
                    return false;
                });
    }
}
