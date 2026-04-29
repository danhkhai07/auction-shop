package com.shop.config;

import com.shop.dto.request.UploadAuctionRequest;
import com.shop.filter.AuthFilter;
import com.shop.filter.RoleFilter;
import com.shop.handler.*;
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
        UploadHandler uploadHandler,

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
                        .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON),
                                builder1 -> builder1
                                        .filter(roleFilter)
                                        .GET("/{id}", viewHandler::getUser)
                                        .POST("/delete/{id}", deleteHandler::deleteUser)
                        )
                        .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON),
                                builder1 -> builder1
                                        .filter(authFilter)
                                        .POST("", authHandler::me)
                        )
                )
                .path("/item", builder -> builder
                        .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON),
                                builder1 -> builder1
                                        .filter(roleFilter)
                                        .GET("/{id}", viewHandler::getItem)
                                        .POST("/delete/{id}", deleteHandler::deleteItem)
                        )
                        .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON),
                                builder1 -> builder1
//                                        .filter(authFilter)
                                        .POST("", uploadHandler::uploadItem)
                                        .POST("/{id}", uploadHandler::updateItem)
                        )
                )
                .path("/auction", builder -> builder
                        .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON),
                                builder1 -> builder1
                                        .filter(roleFilter)
                                        .GET("/{id}", viewHandler::getAuction)
                                        .POST("/delete/{id}", deleteHandler::deleteAuction)
                        )
                        .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON),
                                builder1 -> builder1
                                        .filter(authFilter)
                                        .POST("", uploadHandler::uploadAuction)
                                        .POST("/{id}", uploadHandler::uploadAuction)
                        )
                )
                .build()
        ;
    }
}
