package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Item;
import com.shop.domain.Role;
import com.shop.domain.User;
import com.shop.dto.request.UploadItemRequest;
import com.shop.dto.response.GetAuctionResponse;
import com.shop.dto.response.GetItemResponse;
import com.shop.dto.response.IDResponse;
import de.huxhorn.sulky.ulid.ULID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ItemService {
    private static final String ALL_ITEMS_CACHE_KEY = "items$all";

    private final ItemRepository itemRepository;
    private final UserManager userManager;
    private final CacheManager<Object, Object> cacheManager;
    private final ULID ulid;

    public Mono<Item> getItemByID(String id) {
        return cacheManager.getAs(id, Item.class)
                .map(Mono::just)
                .orElseGet(() -> itemRepository.getByID(id)
                        .doOnNext(item -> cacheManager.put(id, item)))
                .switchIfEmpty(Mono.error(new IllegalAccessException("item not found")));
    }

    public Mono<GetItemResponse> getItemResponseByID(String id) {
        return getItemByID(id)
                .switchIfEmpty(Mono.error(new IllegalAccessException("item not found")))
                .map(this::toResponse);
    }

    public Mono<List<GetItemResponse>> getAllItems(){
        return cachedList(ALL_ITEMS_CACHE_KEY, GetItemResponse.class)
                .map(Mono::just)
                .orElseGet(() -> itemRepository.getAll()
                        .map(GetItemResponse::new)
                        .collectList()
                        .doOnNext(list -> cacheManager.put(ALL_ITEMS_CACHE_KEY, list)));
    }

    private GetItemResponse toResponse(Item item) {
        return new GetItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getSeller().getId()
        );
    }

    public Mono<Void> deleteItem(String id, String deleterID, Set<Role> deleterRoles){
        // can only delete if user owns the item or is admin
        if (deleterRoles == null) return Mono.error(new IllegalAccessException("unauthorized"));
        boolean deleterIsAdmin = deleterRoles.contains(Role.ADMIN);
        boolean deleterIsUser = deleterRoles.contains(Role.USER);
        if (!deleterIsUser && !deleterIsAdmin) return Mono.error(new IllegalAccessException("unauthorized"));

        return cacheManager.getAs(id, Item.class)
                .map(Mono::just)
                .orElseGet(() -> itemRepository.getByID(id))
                .switchIfEmpty(Mono.error(new IllegalStateException("item does not exist")))
                .filter(item -> (item.getSeller().getId().equals(deleterID) || deleterIsAdmin))
                .switchIfEmpty(Mono.error(new IllegalAccessException("unauthorized")))
                .flatMap(b -> itemRepository.deleteByID(id))
                .then(Mono.fromRunnable(() -> {
                    cacheManager.delete(id);
                    cacheManager.delete(ALL_ITEMS_CACHE_KEY);
                }));
    }

    public Mono<IDResponse> newItem(String posterID, UploadItemRequest request) {
        String id = ulid.nextULID();
        if (!posterID.equals(request.sellerID()))
            return Mono.error(new IllegalAccessException("poster is not item owner"));
        return userManager.getUserByID(request.sellerID())
                .flatMap(owner -> {
                    Item item = new Item(
                            id,
                            request.name(),
                            request.description(),
                            owner
                    );
                    return itemRepository.newItem(item)
                            .then(Mono.<Void>fromRunnable(() -> {
                                cacheManager.put(item.getId(), item);
                                cacheManager.delete(ALL_ITEMS_CACHE_KEY);
                                owner.addItem(item);
                            }))
                            .then(userManager.updateUser(owner));
                })
                .thenReturn(new IDResponse(id));
    }

    public Mono<Void> updateItem(String id, String posterID, UploadItemRequest request) {
        if (!posterID.equals(request.sellerID()))
            return Mono.error(new IllegalAccessException("poster is not item owner"));
        return userManager.getUserByID(request.sellerID())
                .flatMap(owner -> {
                    Item item = new Item(
                            id,
                            request.name(),
                            request.description(),
                            owner
                    );
                    return itemRepository.saveItem(item)
                            .then(Mono.fromRunnable(() -> {
                                cacheManager.put(id, item);
                                cacheManager.delete(ALL_ITEMS_CACHE_KEY);
                            }));
                });
    }

    public Mono<Void> transferItemToUser(Item item, User newOwner) {
        if (item == null || newOwner == null) {
            return Mono.error(new IllegalArgumentException("item and new owner are required"));
        }

        Item transferredItem = new Item(
                item.getId(),
                item.getName(),
                item.getDescription(),
                newOwner
        );

        return itemRepository.saveItem(transferredItem)
                .then(Mono.fromRunnable(() -> {
                    cacheManager.put(transferredItem.getId(), transferredItem);
                    cacheManager.delete(ALL_ITEMS_CACHE_KEY);
                    cacheManager.delete(item.getSeller().getId());
                    cacheManager.delete(newOwner.getId());
                }));
    }

    private <T> java.util.Optional<List<T>> cachedList(String key, Class<T> itemType) {
        return cacheManager.getAs(key, List.class)
                .filter(list -> list.stream().allMatch(itemType::isInstance))
                .map(list -> list.stream().map(itemType::cast).toList());
    }
}
