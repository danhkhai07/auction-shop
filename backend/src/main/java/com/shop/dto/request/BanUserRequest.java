package com.shop.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BanUserRequest(
        @JsonProperty("reason") String reason
) {}
