package com.shop.config;

import com.shop.application.AuthService;
import com.shop.handler.AuthFilter;
import com.shop.handler.AuthHandler;
import com.shop.handler.IndexHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterConfig {

    @Bean
    RouterFunction<ServerResponse> routes(
        IndexHandler indexHandler,
        AuthHandler authHandler,
        AuthFilter authFilter
    ) {
        return RouterFunctions.route()
                // Index
                .GET("/", indexHandler::index)
                // Auth
                .path("/auth", builder -> builder
                        .POST("/register", authHandler::register)
                        .POST("/login", authHandler::login)
                        .filter(authFilter)
                        .GET("/me", authHandler::me)
                )
                .build()
        ;
    }
}
