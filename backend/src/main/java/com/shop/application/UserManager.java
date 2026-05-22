package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Auction;
import com.shop.domain.Item;
import com.shop.domain.Role;
import com.shop.domain.User;
import com.shop.dto.response.GetAuctionResponse;
import com.shop.dto.response.GetItemResponse;
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
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    private String addNameCachePrefix(String name) {
        return "username$" + name;
    }

    private void evictUserCache(User user) {
        cacheManager.delete(user.getId());
        cacheManager.delete(addNameCachePrefix(user.getUsername()));
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
            stream = userRepository.getByName(name)
                    .doOnNext(user -> cacheManager.put(key, user));
        }

        return stream.switchIfEmpty(Mono.error(new IllegalAccessException("user not found")));
    }

    public Mono<GetUserResponse> getUserResponseByID(String id){
        return getUserByID(id)
                .switchIfEmpty(Mono.error(new IllegalAccessException("user not found")))
                .map(user -> new GetUserResponse(user));
    }

    public Mono<List<GetUserResponse>> getAllUsers(){
        return userRepository.getAll()
                .map(GetUserResponse::new)
                .collectList();
    }

    private GetItemResponse toItemResponse(Item item) {
        return new GetItemResponse(item);
    }

    private GetAuctionResponse toAuctionResponse(Auction auction) {
        return new GetAuctionResponse(auction);
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
                .doOnNext(v -> {
                    cacheManager.put(user.getId(), user);
                    cacheManager.put(addNameCachePrefix(user.getUsername()), user);
                });
    }

    public Mono<Void> updateUser(User user){
        return userRepository.saveUser(user)
                .doOnNext(v -> {
                    cacheManager.put(user.getId(), user);
                    cacheManager.put(addNameCachePrefix(user.getUsername()), user);
                });
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
}
