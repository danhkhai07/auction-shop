package com.shop.dto.response;

import com.shop.domain.Role;

import java.util.Set;

public class GetUserResponse {
    public final String id;
    public final String username;
    public final Set<Role> roles;

    public GetUserResponse(
            String id,
            String username,
            Set<Role> roles
    ) {
        this.id = id;
        this.username = username;
        this.roles = roles;
    }
}
