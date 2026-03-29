package com.shop.app;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(ServerErrorException.class)
    public Mono<Map<String, String>> handleValidation(ServerWebInputException ex) {
        return Mono.just(Map.of(
            "error", "Invalid request",
            "message", ex.getReason()
        ));
    }

    @ExceptionHandler(Exception.class)
    public Mono<Map<String, String>> handleGeneral(Exception ex) {
        return Mono.just(Map.of(
            "error", "Internal error",
            "message", ex.getMessage()
        ));
    }
}
