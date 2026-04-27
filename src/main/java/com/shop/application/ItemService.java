package com.shop.application;

import com.shop.cache.CacheManager;
import com.shop.domain.Item;
import com.shop.dto.response.GetItemResponse;
import com.shop.infra.InMemoryCacheStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
}
