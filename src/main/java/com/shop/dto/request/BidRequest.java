package com.shop.dto.request;

import java.math.BigDecimal;

public record BidRequest(
    BigDecimal amount
) {}
