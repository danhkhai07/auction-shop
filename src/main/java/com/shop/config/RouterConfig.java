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

import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;

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
                        .POST("/register", contentType(MediaType.APPLICATION_JSON), authHandler::register)
                        .POST("/login", contentType(MediaType.APPLICATION_JSON), authHandler::login)
                        .GET("/me", authHandler::me).filter(authFilter)
                )
                .path("/user", builder -> builder
                        .GET("/{id}", viewHandler::getUser)
                        .POST("/delete/{id}", contentType(MediaType.APPLICATION_JSON), deleteHandler::deleteUser).filter(roleFilter)
                )
                .path("/item", builder -> builder
                        .GET("/{id}", viewHandler::getItem)
                        .POST("/delete/{id}", contentType(MediaType.APPLICATION_JSON), deleteHandler::deleteItem).filter(roleFilter)
                        .POST("", contentType(MediaType.APPLICATION_JSON), uploadHandler::uploadItem).filter(authFilter)
                        .POST("/{id}", contentType(MediaType.APPLICATION_JSON), uploadHandler::updateItem).filter(authFilter)
                )
                .path("/auction", builder -> builder
                        .GET("/{id}", viewHandler::getAuction)
                        .POST("/delete/{id}", contentType(MediaType.APPLICATION_JSON), deleteHandler::deleteAuction).filter(roleFilter)
                        .POST("", contentType(MediaType.APPLICATION_JSON), uploadHandler::uploadAuction).filter(authFilter)
                        .path("/{id}", builder2 -> builder2
                                .POST("", contentType(MediaType.APPLICATION_JSON), uploadHandler::updateAuction).filter(authFilter)
                                .POST("/bid", contentType(MediaType.APPLICATION_JSON), auctionHandler::placeBid).filter(authFilter)
                                .POST("/start", contentType(MediaType.APPLICATION_JSON), auctionHandler::startAuction).filter(authFilter)
                                .POST("/pause", contentType(MediaType.APPLICATION_JSON), auctionHandler::pauseAuction).filter(authFilter)
                                .POST("/unpause", contentType(MediaType.APPLICATION_JSON), auctionHandler::unpauseAuction).filter(authFilter)
                                .POST("/cancel", contentType(MediaType.APPLICATION_JSON), auctionHandler::cancelAuction).filter(authFilter)
                                .POST("/end", contentType(MediaType.APPLICATION_JSON), auctionHandler::finishAuction).filter(authFilter)
                                .POST("/extend/endtime", contentType(MediaType.APPLICATION_JSON), auctionHandler::extendEndtime).filter(authFilter)
                        )
                )
                .path("/feed", builder -> builder
                        .GET("", viewHandler::getFeed)
                )
                .build();
    }
}
