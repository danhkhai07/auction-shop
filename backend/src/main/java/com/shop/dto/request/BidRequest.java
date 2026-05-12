package com.shop.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record BidRequest(
    @JsonProperty("amount") BigDecimal amount
) {
    public boolean hasEmptyFields() {
        return (amount == null);
    }
}
