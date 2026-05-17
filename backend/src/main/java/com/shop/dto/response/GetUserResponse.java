package com.shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shop.domain.Role;

import java.util.List;
import java.util.Set;

public record GetUserResponse(
    @JsonProperty("id") String id,
    @JsonProperty("username") String username,
    @JsonProperty("roles") Set<Role> roles,
    @JsonProperty("itemList") List<GetItemResponse> itemList,
    @JsonProperty("auctionList") List<GetAuctionResponse> auctionList
){}
