package com.shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shop.domain.Role;
import com.shop.domain.User;

import java.util.List;
import java.util.Set;

public record GetUserResponse(
    @JsonProperty("id") String id,
    @JsonProperty("username") String username,
    @JsonProperty("roles") Set<Role> roles,
    @JsonProperty("itemList") List<String> itemList,
    @JsonProperty("auctionList") List<String> auctionList
){
    public GetUserResponse(User user) {
        this(user.getId(),
            user.getUsername(),
            user.getRoles(),
            user.getOwnedItemIds(),
            user.getOwnedAuctionIds());
    }
}