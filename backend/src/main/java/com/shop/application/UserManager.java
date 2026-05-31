package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Role;
import com.shop.domain.User;
import com.shop.dto.response.GetUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserManager {
    private static final String ALL_USERS_CACHE_KEY = "users$all";

    private final UserRepository userRepository;
    private final CacheManager<Object, Object> cacheManager;

    private String addNameCachePrefix(String name) {
        return "username$" + name;
    }

    private void evictUserCache(User user) {
        cacheManager.delete(user.getId());
        cacheManager.delete(addNameCachePrefix(user.getUsername()));
        cacheManager.delete(ALL_USERS_CACHE_KEY);
    }

    public Mono<User> getUserByID(String id){
        return cacheManager.getAs(id, User.class)
                .map(Mono::just)
                .orElseGet(() -> userRepository.getByID(id)
                        .doOnNext(user -> cacheManager.put(id, user)))
                .switchIfEmpty(Mono.error(new IllegalAccessException("user not found")));
    }

    public Mono<User> getUserByName(String name){
        String key = addNameCachePrefix(name);
        return cacheManager.getAs(key, User.class)
                .map(Mono::just)
                .orElseGet(() -> userRepository.getByName(name)
                        .doOnNext(user -> cacheManager.put(key, user)))
                .switchIfEmpty(Mono.error(new IllegalAccessException("user not found")));
    }

    public Mono<GetUserResponse> getUserResponseByID(String id){
        return getUserByID(id)
                .switchIfEmpty(Mono.error(new IllegalAccessException("user not found")))
                .map(user -> new GetUserResponse(user));
    }

    public Mono<List<GetUserResponse>> getAllUsers(){
        return cachedList(ALL_USERS_CACHE_KEY, GetUserResponse.class)
                .map(Mono::just)
                .orElseGet(() -> userRepository.getAll()
                        .map(GetUserResponse::new)
                        .collectList()
                        .doOnNext(list -> cacheManager.put(ALL_USERS_CACHE_KEY, list)));
    }

    public Mono<Void> deleteUser(String id, String deleterID, Set<Role> deleterRoles){
        // can only delete if user is self or is admin
        if (deleterRoles == null) return Mono.error(new IllegalAccessException("unauthorized"));
        boolean deleterIsSelf = deleterID.equals(id);
        boolean deleterIsAdmin = deleterRoles.contains(Role.ADMIN);
        if (!deleterIsSelf && !deleterIsAdmin) return Mono.error(new IllegalAccessException("unauthorized"));

        return this.getUserByID(id)
                .flatMap(user -> {
                    evictUserCache(user);
                    return userRepository.deleteByID(id);
                });
    }

    public Mono<Void> newUser(User user){
        return userRepository.newUser(user)
                .onErrorResume(DuplicateKeyException ->
                        Mono.error(new IllegalStateException("user already exists")))
                .then(Mono.fromRunnable(() -> {
                    cacheManager.put(user.getId(), user);
                    cacheManager.put(addNameCachePrefix(user.getUsername()), user);
                    cacheManager.delete(ALL_USERS_CACHE_KEY);
                }));
    }

    public Mono<Void> updateUser(User user){
        return userRepository.saveUser(user)
                .then(Mono.fromRunnable(() -> {
                    cacheManager.put(user.getId(), user);
                    cacheManager.put(addNameCachePrefix(user.getUsername()), user);
                    cacheManager.delete(ALL_USERS_CACHE_KEY);
                }));
    }

    public Mono<Void> elevateUser(String id){
        return this.getUserByID(id)
                .flatMap(user -> {
                    user.addRole(Role.ADMIN);
                    return this.updateUser(user);
                });
    }

    public Mono<Void> banUser(String targetUserId, String adminId, String reason) {
        if (targetUserId == null || targetUserId.isBlank()) {
            return Mono.error(new IllegalArgumentException("user id is invalid"));
        }
        if (adminId.equals(targetUserId)) {
            return Mono.error(new IllegalAccessException("cannot ban yourself"));
        }

        return this.getUserByID(targetUserId)
                .flatMap(user -> userRepository.banByID(targetUserId, reason, adminId)
                        .then(Mono.fromRunnable(() -> evictUserCache(user))));
    }

    public Mono<Void> unbanUser(String targetUserId, String adminId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            return Mono.error(new IllegalArgumentException("user id is invalid"));
        }
        if (adminId.equals(targetUserId)) {
            return Mono.error(new IllegalAccessException("cannot unban yourself"));
        }

        return this.getUserByID(targetUserId)
                .flatMap(user -> userRepository.unbanByID(targetUserId)
                        .then(Mono.fromRunnable(() -> evictUserCache(user))));
    }

    public Mono<Void> addBalance(String id, java.math.BigDecimal amount) {
        return userRepository.addBalance(id, amount)
                .then(userRepository.getByID(id))
                .flatMap(user -> Mono.fromRunnable(() -> {
                     cacheManager.put(user.getId(), user);
                     cacheManager.put(addNameCachePrefix(user.getUsername()), user);
                     cacheManager.delete(ALL_USERS_CACHE_KEY);
                }));
    }

    public Mono<Void> deductBalance(String id, java.math.BigDecimal amount) {
        return userRepository.deductBalance(id, amount)
                .then(userRepository.getByID(id))
                .flatMap(user -> Mono.fromRunnable(() -> {
                     cacheManager.put(user.getId(), user);
                     cacheManager.put(addNameCachePrefix(user.getUsername()), user);
                     cacheManager.delete(ALL_USERS_CACHE_KEY);
                }));
    }

    private <T> java.util.Optional<List<T>> cachedList(String key, Class<T> itemType) {
        return cacheManager.getAs(key, List.class)
                .filter(list -> list.stream().allMatch(itemType::isInstance))
                .map(list -> list.stream().map(itemType::cast).toList());
    }
}
