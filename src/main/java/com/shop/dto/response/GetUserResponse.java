package com.shop.dto.response;

import com.shop.domain.Role;

import java.util.List;
import java.util.Set;

public record GetUserResponse(
    String id,
    String username,
    Set<Role> roles,
    List<String> itemList,
    List<String> auctionList
){}