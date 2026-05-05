package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Role;
import com.shop.domain.User;
import com.shop.dto.response.GetUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserManager {
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    private String addNameCachePrefix(String name) {
        return "username$" + name;
    }

    public Mono<User> getUserByID(String id){
        Mono<User> stream;
        if (cacheManager.contains(id)) {
            stream = Mono.justOrEmpty(cacheManager.get(id))
                    .filter(obj -> obj instanceof User)
                    .cast(User.class);
        } else {
            stream = userRepository.getByID(id)
                    .doOnNext(user -> cacheManager.put(id, user));
        }

        return stream.switchIfEmpty(Mono.error(new IllegalAccessException("user not found")));
    }

    public Mono<User> getUserByName(String name){
        Mono<User> stream;
        String key = addNameCachePrefix(name);
        if (cacheManager.contains(key)) {
            stream = Mono.justOrEmpty(cacheManager.get(key))
                    .filter(obj -> obj instanceof User)
                    .cast(User.class);
        } else {
            stream = userRepository.getByID(key)
                    .doOnNext(user -> cacheManager.put(key, user));
        }

        return stream.switchIfEmpty(Mono.error(new IllegalAccessException("user not found")));
    }

    public Mono<GetUserResponse> getUserResponseByID(String id){
        return getUserByID(id)
                .switchIfEmpty(Mono.error(new IllegalAccessException("user not found")))
                .map(user -> {
                    GetUserResponse response = new GetUserResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getRoles()
                    );
                    return response;
                });
    }

    public Mono<Void> deleteUser(String id, String deleterID, Set<Role> deleterRoles){
        // can only delete if user is self or is admin
        if (deleterRoles == null) return Mono.error(new IllegalAccessException("unauthorized"));
        boolean deleterIsSelf = deleterID.equals(id);
        boolean deleterIsAdmin = deleterRoles.contains(Role.ADMIN);
        if (!deleterIsSelf && !deleterIsAdmin) return Mono.error(new IllegalAccessException("unauthorized"));

        Mono<String> stream; // Mono<id>
        if (cacheManager.contains(id)) {
            stream = Mono.fromRunnable(() -> cacheManager.delete(id))
                    .thenReturn(id);
        } else {
            stream = userRepository.existsByID(id)
                    .filter(b -> b)
                    .thenReturn(id);
        }

        return stream
                .switchIfEmpty(Mono.error(new IllegalStateException("user does not exist")))
                .flatMap(userRepository::deleteByID);
    }

    public Mono<Void> newUser(User user){
        cacheManager.put(user.getId(), user);
        cacheManager.put(addNameCachePrefix(user.getUsername()), user);
        return userRepository.newUser(user);
    }
}
