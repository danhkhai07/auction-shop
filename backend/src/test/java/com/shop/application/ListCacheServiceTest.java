package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Auction;
import com.shop.domain.Item;
import com.shop.domain.User;
import com.shop.dto.request.UploadItemRequest;
import com.shop.dto.response.GetAuctionResponse;
import com.shop.dto.response.GetItemResponse;
import com.shop.dto.response.GetUserResponse;
import com.shop.infra.InMemoryCacheStore;
import de.huxhorn.sulky.ulid.ULID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListCacheServiceTest {

    @Test
    void auctionListIsCachedAfterFirstRead() {
        AuctionRepository auctionRepository = mock(AuctionRepository.class);
        Auction auction = auction("auction-1", item("item-1", user("seller-1")));
        when(auctionRepository.getAll()).thenReturn(Flux.just(auction));

        AuctionService service = new AuctionService(
                auctionRepository,
                null,
                null,
                null,
                cacheManager()
        );

        StepVerifier.create(service.getAllAuctions())
                .assertNext(list -> assertThat(list).extracting(GetAuctionResponse::id).containsExactly("auction-1"))
                .verifyComplete();
        StepVerifier.create(service.getAllAuctions())
                .assertNext(list -> assertThat(list).extracting(GetAuctionResponse::id).containsExactly("auction-1"))
                .verifyComplete();

        verify(auctionRepository, times(1)).getAll();
    }

    @Test
    void activeAuctionListCacheIsInvalidatedWhenAuctionStatusChanges() {
        AuctionRepository auctionRepository = mock(AuctionRepository.class);
        Auction auction = auction("auction-1", item("item-1", user("seller-1")));
        when(auctionRepository.getActives()).thenReturn(Flux.just(auction));
        when(auctionRepository.saveAuction(auction)).thenReturn(Mono.empty());

        AuctionService service = new AuctionService(
                auctionRepository,
                null,
                null,
                null,
                cacheManager()
        );

        StepVerifier.create(service.getActiveAuctions().collectList())
                .assertNext(list -> assertThat(list).extracting(GetAuctionResponse::id).containsExactly("auction-1"))
                .verifyComplete();
        StepVerifier.create(service.updateAuctionStatus(auction)).verifyComplete();
        StepVerifier.create(service.getActiveAuctions().collectList())
                .assertNext(list -> assertThat(list).extracting(GetAuctionResponse::id).containsExactly("auction-1"))
                .verifyComplete();

        verify(auctionRepository, times(2)).getActives();
    }

    @Test
    void itemListCacheIsInvalidatedWhenNewItemIsCreated() {
        ItemRepository itemRepository = mock(ItemRepository.class);
        User seller = user("seller-1");
        UserManager userManager = new UserManager(new FakeUserRepository(seller), cacheManager());

        when(itemRepository.getAll()).thenReturn(Flux.just(item("old-item", seller)));
        when(itemRepository.newItem(org.mockito.ArgumentMatchers.any(Item.class))).thenReturn(Mono.empty());

        ItemService service = new ItemService(itemRepository, userManager, cacheManager(), new ULID());

        StepVerifier.create(service.getAllItems())
                .assertNext(list -> assertThat(list).extracting(GetItemResponse::id).containsExactly("old-item"))
                .verifyComplete();
        StepVerifier.create(service.newItem("seller-1", new UploadItemRequest("name", "desc", "seller-1")))
                .assertNext(response -> assertThat(response.id()).isNotBlank())
                .verifyComplete();
        StepVerifier.create(service.getAllItems())
                .assertNext(list -> assertThat(list).extracting(GetItemResponse::id).containsExactly("old-item"))
                .verifyComplete();

        verify(itemRepository, times(2)).getAll();
    }

    @Test
    void userListCacheIsInvalidatedWhenUserIsUpdated() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = user("user-1");
        when(userRepository.getAll()).thenReturn(Flux.just(user));
        when(userRepository.saveUser(user)).thenReturn(Mono.empty());

        UserManager userManager = new UserManager(userRepository, cacheManager());

        StepVerifier.create(userManager.getAllUsers())
                .assertNext(list -> assertThat(list).extracting(GetUserResponse::id).containsExactly("user-1"))
                .verifyComplete();
        StepVerifier.create(userManager.updateUser(user)).verifyComplete();
        StepVerifier.create(userManager.getAllUsers())
                .assertNext(list -> assertThat(list).extracting(GetUserResponse::id).containsExactly("user-1"))
                .verifyComplete();

        verify(userRepository, times(2)).getAll();
    }

    private static CacheManager<Object, Object> cacheManager() {
        return new CacheManager<>(new InMemoryCacheStore<>(), 60, 30);
    }

    private static User user(String id) {
        return new User(id, id, "password");
    }

    private static Item item(String id, User seller) {
        return new Item(id, "name-" + id, "desc-" + id, seller);
    }

    private static Auction auction(String id, Item item) {
        return new Auction(
                id,
                item,
                BigDecimal.TEN,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(1)
        );
    }

    private static final class FakeUserRepository implements UserRepository {
        private final User user;

        private FakeUserRepository(User user) {
            this.user = user;
        }

        @Override
        public Mono<Boolean> existsByID(String id) {
            return Mono.just(user.getId().equals(id));
        }

        @Override
        public Flux<User> getAll() {
            return Flux.just(user);
        }

        @Override
        public Mono<User> getByID(String id) {
            return user.getId().equals(id) ? Mono.just(user) : Mono.empty();
        }

        @Override
        public Mono<User> getByName(String name) {
            return user.getUsername().equals(name) ? Mono.just(user) : Mono.empty();
        }

        @Override
        public Mono<Void> saveUser(User user) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> deleteByID(String id) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> newUser(User user) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> changePassword(String id, String password) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> banByID(String id, String reason, String bannedBy) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> unbanByID(String id) {
            return Mono.empty();
        }
    }
}
