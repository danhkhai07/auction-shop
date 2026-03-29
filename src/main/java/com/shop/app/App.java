package com.shop.app;

import com.shop.config.Config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@SpringBootApplication
@RestController
public class App {
    String addr;
//    App(){}

    public void Run(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @GetMapping("/")
    public Mono<Map<String, String>> index() {
        return Mono.just(Map.of(
            "message", "Welcome to auction-shop API!"
        ));
    }
}
