package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Item;
import com.shop.domain.User;
import com.shop.infra.InMemoryCacheStore;
import de.huxhorn.sulky.ulid.ULID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemServiceTest {

    @Test
    void transferItemToUserMovesExistingItemAndInvalidatesSellerAndBuyerCaches() {
        ItemRepository itemRepository = mock(ItemRepository.class);
        CacheManager<Object, Object> cacheManager = new CacheManager<>(new InMemoryCacheStore<>(), 60, 30);
        RecordingUserManager userManager = new RecordingUserManager(cacheManager);
        ItemService service = new ItemService(itemRepository, userManager, cacheManager, new ULID());

        User seller = new User("seller-1", "seller", "password");
        User buyer = new User("buyer-1", "buyer", "password");
        Item item = new Item("item-1", "camera", "dslr", seller);

        cacheManager.put("items$all", java.util.List.of("stale"));
        when(itemRepository.saveItem(any(Item.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.transferItemToUser(item, buyer))
                .verifyComplete();

        var itemCaptor = org.mockito.ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).saveItem(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getId()).isEqualTo("item-1");
        assertThat(itemCaptor.getValue().getSeller().getId()).isEqualTo("buyer-1");

        assertThat(cacheManager.getAs("item-1", Item.class)).contains(itemCaptor.getValue());
        assertThat(cacheManager.contains("items$all")).isFalse();
        assertThat(userManager.evictedUsers).containsExactly(seller, buyer);
        verify(itemRepository, never()).newItem(any());
    }

    private static final class RecordingUserManager extends UserManager {
        private final java.util.List<User> evictedUsers = new java.util.ArrayList<>();

        private RecordingUserManager(CacheManager<Object, Object> cacheManager) {
            super(new NoopUserRepository(), cacheManager);
        }

        @Override
        public void evictUserCache(User user) {
            evictedUsers.add(user);
        }
    }

    private static final class NoopUserRepository implements UserRepository {
        @Override
        public reactor.core.publisher.Mono<Boolean> existsByID(String id) {
            return reactor.core.publisher.Mono.just(false);
        }

        @Override
        public reactor.core.publisher.Flux<User> getAll() {
            return reactor.core.publisher.Flux.empty();
        }

        @Override
        public reactor.core.publisher.Mono<User> getByID(String id) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Mono<User> getByName(String name) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Mono<Void> saveUser(User user) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Mono<Void> deleteByID(String id) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Mono<Void> newUser(User user) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Mono<Void> changePassword(String id, String password) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Mono<Void> banByID(String id, String reason, String bannedBy) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Mono<Void> unbanByID(String id) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Mono<Void> addBalance(String id, java.math.BigDecimal amount) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Mono<Void> deductBalance(String id, java.math.BigDecimal amount) {
            return reactor.core.publisher.Mono.empty();
        }
    }
}
