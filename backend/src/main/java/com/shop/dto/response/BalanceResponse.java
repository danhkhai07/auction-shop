package com.shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record BalanceResponse(
    @JsonProperty("userId") String userId,
    @JsonProperty("username") String username,
    @JsonProperty("balance") BigDecimal balance,
    @JsonProperty("message") String message
) {
    public BalanceResponse(String userId, String username, BigDecimal balance) {
        this(userId, username, balance, null);
    }
}
