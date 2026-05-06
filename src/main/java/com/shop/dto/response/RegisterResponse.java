package com.shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegisterResponse(
    @JsonProperty("message") String message
){}
