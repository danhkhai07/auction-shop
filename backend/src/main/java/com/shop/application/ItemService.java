package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Item;
import com.shop.domain.Role;
import com.shop.dto.request.UploadItemRequest;
import com.shop.dto.response.GetItemResponse;
import com.shop.dto.response.IDResponse;
import de.huxhorn.sulky.ulid.ULID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final UserManager userManager;
    private final CacheManager cacheManager;
    private final ULID ulid;

    public Mono<Item> getItemByID(String id) {
        Mono<Item> stream;
        if (cacheManager.contains(id)) {
            stream = Mono.just(cacheManager.get(id))
                    .filter(obj -> obj instanceof Item)
                    .cast(Item.class);
        } else {
            stream = itemRepository.getByID(id)
                    .doOnNext(item -> cacheManager.put(id, item));
        }

        return stream.switchIfEmpty(Mono.error(new IllegalAccessException("item not found")));
    }

    public Mono<GetItemResponse> getItemResponseByID(String id) {
        return getItemByID(id)
                .switchIfEmpty(Mono.error(new IllegalAccessException("item not found")))
                .map(item -> {
                    GetItemResponse response = new GetItemResponse(
                            item.getId(),
                            item.getName(),
                            item.getDescription(),
                            item.getSeller().getId()
                    );
                    return response;
                });
    }

    public Mono<Void> deleteItem(String id, String deleterID, Set<Role> deleterRoles){
        // can only delete if user owns the item or is admin
        if (deleterRoles == null) return Mono.error(new IllegalAccessException("unauthorized"));
        boolean deleterIsAdmin = deleterRoles.contains(Role.ADMIN);
        boolean deleterIsUser = deleterRoles.contains(Role.USER);
        if (!deleterIsUser && !deleterIsAdmin) return Mono.error(new IllegalAccessException("unauthorized"));

        Mono<Item> stream;
        if (cacheManager.contains(id)) {
            stream = Mono.just(cacheManager.get(id))
                    .filter(obj -> obj instanceof Item)
                    .cast(Item.class);
        } else {
            stream = itemRepository.getByID(id);
        }

        return stream
                .switchIfEmpty(Mono.error(new IllegalStateException("item does not exist")))
                .filter(item -> (item.getSeller().getId().equals(deleterID) || deleterIsAdmin))
                .switchIfEmpty(Mono.error(new IllegalAccessException("unauthorized")))
                .flatMap(b -> itemRepository.deleteByID(id))
                .doOnNext(v -> {
                        cacheManager.delete(id);
                });
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
                    return itemRepository.newItem(item);
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
                    if (cacheManager.contains(id)) {
                        cacheManager.put(id, item);
                    }
                    return itemRepository.saveItem(item);
                })
                .switchIfEmpty(Mono.error(new IllegalStateException("item does not exists")));
    }
}
