package com.shop.config;

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
        AuctionHandler auctionHandler,

        AuthFilter authFilter,
        RoleFilter roleFilter
    ) {
        return RouterFunctions.route()
                .GET("/", indexHandler::index)
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
                        .GET("/{id}", viewHandler::getUser)
                        .nest(RequestPredicates.contentType(MediaType.APPLICATION_JSON),
                                builder1 -> builder1
                                        .filter(roleFilter)
                                        .POST("/delete/{id}", deleteHandler::deleteUser)
                        )
//                        .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON),
//                                builder1 -> builder1
//                                        .filter(authFilter)
//                                        .POST("", ::)
//                        )
                )
                .path("/item", builder -> builder
                        .nest(RequestPredicates.contentType(MediaType.APPLICATION_JSON),
                                builder1 -> builder1
                                        .GET("/{id}", viewHandler::getItem)
                                        .POST("/delete/{id}", deleteHandler::deleteItem)
                                        .POST("", uploadHandler::uploadItem)
                                        .POST("/{id}", uploadHandler::updateItem)
                                        .filter((request, next) -> {
                                            String path = request.path();
                                            String method = request.methodName();
                                            if (method.equals("POST") && path.contains("/delete/")) {
                                                return roleFilter.filter(request, next);
                                            }
                                            if (method.equals("POST") && (path.equals("/item") || path.matches("/item/[^/]+"))) {
                                                return authFilter.filter(request, next);
                                            }
                                            return next.handle(request);
                                        })
                        )
                )
                .path("/auction", builder -> builder
                        .GET("/{id}", viewHandler::getAuction)
                        .nest(RequestPredicates.contentType(MediaType.APPLICATION_JSON),
                                builder1 -> builder1
                                        .POST("/delete/{id}", deleteHandler::deleteAuction)
                                        .POST("", uploadHandler::uploadAuction)
                                        .path("/{id}", builder2 -> builder2
                                                .POST("", uploadHandler::updateAuction)
                                                .POST("/bid", auctionHandler::placeBid)
                                                .POST("/start", auctionHandler::startAuction)
                                                .POST("/pause", auctionHandler::pauseAuction)
                                                .POST("/unpause", auctionHandler::unpauseAuction)
                                                .POST("/cancel", auctionHandler::cancelAuction)
                                                .POST("/end", auctionHandler::finishAuction)
                                                .POST("/extend/endtime", auctionHandler::extendEndtime)
                                        )
                                        .filter((request, next) -> {
                                            String path = request.path();
                                            String method = request.methodName();
                                            if (method.equals("POST") && path.contains("/delete/")) {
                                                return roleFilter.filter(request, next);
                                            }
                                            if (method.equals("POST")) {
                                                return authFilter.filter(request, next);
                                            }
                                            return next.handle(request);
                                        })
                        )
                )
                .path("/feed", builder -> builder
                        .GET("", viewHandler::getFeed)
                )
                .build()
        ;
    }
}
