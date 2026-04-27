package com.shop.config;

import com.shop.filter.AuthFilter;
import com.shop.filter.RoleFilter;
import com.shop.handler.AuthHandler;
import com.shop.handler.DeleteHandler;
import com.shop.handler.IndexHandler;
import com.shop.handler.ViewHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterConfig {

    @Bean
    RouterFunction<ServerResponse> routes(
        IndexHandler indexHandler,
        AuthHandler authHandler,
        ViewHandler viewHandler,
        DeleteHandler deleteHandler,

        AuthFilter authFilter,
        RoleFilter roleFilter
    ) {
        return RouterFunctions.route()
                // Index
                .GET("/", indexHandler::index)
                // Auth
                .path("/auth", builder -> builder
                        .POST("/register", authHandler::register)
                        .POST("/login", authHandler::login)
                        .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON),
                            builder1 -> builder1
                            .GET("/me", authHandler::me)
                            .filter(authFilter)
                        )
                )
                .path("/user", builder -> builder
                        .filter(roleFilter)
                        .GET("/{id}", viewHandler::getUser)
                        .POST("/delete/{id}", deleteHandler::deleteUser)
                )
                .path("/item", builder -> builder
                        .filter(roleFilter)
                        .GET("/{id}", viewHandler::getItem)
                        .POST("/delete/{id}", deleteHandler::deleteItem)
                )
                .path("/auction", builder -> builder
                        .filter(roleFilter)
                        .GET("/{id}", viewHandler::getAuction)
                        .POST("/delete/{id}", deleteHandler::deleteAuction)
                )
                .build()
        ;
    }
}
