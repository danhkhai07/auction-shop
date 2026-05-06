package com.shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IDResponse(
    @JsonProperty("id") String id
){}
