package com.shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shop.domain.Role;
import com.shop.domain.User;

import java.util.List;
import java.util.Set;

public record GetShortUserResponse(
    @JsonProperty("id") String id,
    @JsonProperty("username") String username,
    @JsonProperty("roles") Set<Role> roles
){
    public GetShortUserResponse(User user) {
        this(user.getId(),
            user.getUsername(),
            user.getRoles());
    }
}