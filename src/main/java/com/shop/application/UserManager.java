package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.cache.CacheStore;
import com.shop.domain.User;
import com.shop.dto.response.GetUserResponse;
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

    public Mono<GetUserResponse> getUserByID(String id){
        Mono<User> stream;
        if (IDCache.contains(id)) {
            stream = Mono.just(IDCache.get(id))
                    .filter(obj -> obj instanceof User)
                    .cast(User.class);
        } else {
            stream = userRepository.getByID(id)
                    .filter(user -> {
                        IDCache.put(id, user);
                        return true;
                    });
        }

        return stream
                .switchIfEmpty(Mono.error(new IllegalAccessException("user not found")))
                .map(user -> {
                    GetUserResponse response = new GetUserResponse(
                            user.id,
                            user.username,
                            user.getRoles()
                    );
                    return response;
                });
    }

    public Mono<GetUserResponse> getUserByName(String name){
        Mono<User> stream;
        if (IDCache.contains(name)) {
            stream = Mono.just(NameCache.get(name))
                    .filter(obj -> obj instanceof User)
                    .cast(User.class);
        } else {
            stream = userRepository.getByName(name)
                    .filter(user -> {
                        NameCache.put(name, user);
                        return true;
                    });
        }

        return stream
                .switchIfEmpty(Mono.error(new IllegalAccessException("user not found")))
                .map(user -> {
                    GetUserResponse response = new GetUserResponse(
                            user.id,
                            user.username,
                            user.getRoles()
                    );
                    return response;
                });
    }
}
