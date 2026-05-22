package com.shop.config;

import com.shop.filter.AuthFilter;
import com.shop.filter.RoleFilter;
import com.shop.handler.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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
        AdminActionsHandler elevateUserHandler,

        AuthFilter authFilter,
        RoleFilter roleFilter
    ) {
        return RouterFunctions.route()
                .GET("/", indexHandler::index)
                .path("/auth", builder -> builder
                        .POST("/register", contentType(MediaType.APPLICATION_JSON), authHandler::register)
                        .POST("/login", contentType(MediaType.APPLICATION_JSON), authHandler::login)
                )
                .path("/auth", builder -> builder
                        .GET("/me", authHandler::me).filter(authFilter)
                )
                .path("/user", builder -> builder
                        .GET("/{id}", viewHandler::getUser)
                        .POST("/delete/{id}", deleteHandler::deleteUser).filter(roleFilter)
                )
                .path("/item", builder -> builder
                        .GET("/{id}", viewHandler::getItem)
                        .POST("/delete/{id}", deleteHandler::deleteItem).filter(roleFilter)
                        .POST("", contentType(MediaType.APPLICATION_JSON), uploadHandler::uploadItem).filter(authFilter)
                        .POST("/{id}", contentType(MediaType.APPLICATION_JSON), uploadHandler::updateItem).filter(authFilter)
                )
                .path("/auction", builder -> builder
                        .POST("/delete/{id}", deleteHandler::deleteAuction).filter(roleFilter)
                        .POST("", contentType(MediaType.APPLICATION_JSON), uploadHandler::uploadAuction).filter(authFilter)
                        .path("/{id}", secured -> secured
                                .POST("", contentType(MediaType.APPLICATION_JSON), uploadHandler::updateAuction).filter(authFilter)
                                .POST("/bid", contentType(MediaType.APPLICATION_JSON), auctionHandler::placeBid).filter(authFilter)
                                .POST("/start", auctionHandler::startAuction).filter(authFilter)
                                .POST("/pause", auctionHandler::pauseAuction).filter(authFilter)
                                .POST("/unpause", auctionHandler::unpauseAuction).filter(authFilter)
                                .POST("/cancel", auctionHandler::cancelAuction).filter(authFilter)
                                .POST("/end", auctionHandler::finishAuction).filter(authFilter)
                                .POST("/extend/endtime", contentType(MediaType.APPLICATION_JSON), auctionHandler::extendEndtime).filter(authFilter)
                        )
                )
                .path("/auction", builder -> builder
                        .GET("/{id}", viewHandler::getAuction)
                        .GET("/{id}/events", auctionHandler::stream)
                )
                .path("/feed", builder -> builder
                        .GET("", viewHandler::getFeed)
                )
                .path("/admin", builder -> builder
                        .filter(roleFilter)
                        .POST("/elevate/user/{id}", elevateUserHandler::elevateUser)
                )
                .build();
    }
}
