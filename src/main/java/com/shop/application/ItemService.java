package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Item;
import com.shop.domain.Role;
import com.shop.domain.User;
import com.shop.dto.response.GetItemResponse;
import com.shop.infra.InMemoryCacheStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final CacheManager IDCache = new CacheManager(
            new InMemoryCacheStore<String, Item>(),
            3 * 60,
            10 * 60
    );

    public Mono<GetItemResponse> getItemByID(String id) {
        Mono<Item> stream;
        if (IDCache.contains(id)) {
            stream = Mono.just(IDCache.get(id))
                    .filter(obj -> obj instanceof Item)
                    .cast(Item.class);
        } else {
            stream = itemRepository.getByID(id)
                    .filter(item -> {
                        IDCache.put(id, item);
                        return true;
                    });
        }

        return stream
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
        if (IDCache.contains(id)) {
            stream = Mono.just((Item) IDCache.get(id));
        } else {
            stream = itemRepository.existsByID(id)
                    .filter(b -> b)
                    .flatMap(b -> itemRepository.getByID(id));
        }

        return stream
                .switchIfEmpty(Mono.error(new IllegalStateException("item does not exist")))
                .filter(item -> (item.getSeller().getId().equals(deleterID) || deleterIsAdmin))
                .switchIfEmpty(Mono.error(new IllegalAccessException("unauthorized")))
                .flatMap(b -> itemRepository.deleteByID(id))
                .filter(v -> {
                    IDCache.delete(id);
                    return true;
                });
    }
}
