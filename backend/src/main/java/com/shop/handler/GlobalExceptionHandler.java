package com.shop.handler;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        HttpStatus status;
        String message = ex.getMessage();

        if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof IllegalAccessException) {
            status = HttpStatus.FORBIDDEN;
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "internal error";
        }

        byte[] bytes = toJson(Map.of(
                "error", ex.getClass().getSimpleName(),
                "message", message
        ));

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(bytes)));
    }

    private byte[] toJson(Map<String, Object> body) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsBytes(body);
        } catch (Exception e) {
            return "{\"error\":\"serialization failed\"}".getBytes(StandardCharsets.UTF_8);
        }
    }
}
