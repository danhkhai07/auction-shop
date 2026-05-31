package com.shop.handler;

import com.shop.application.ReviewService;
import com.shop.dto.request.ReviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ReviewHandler {

    private final ReviewService reviewService;

    public Mono<ServerResponse> createReview(ServerRequest request) {
        return request.attribute("userID")
                .map(Object::toString)
                .map(userId -> request.bodyToMono(ReviewRequest.class)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("missing body")))
                        .flatMap(req -> reviewService.createReview(userId, req))
                        .flatMap(response -> ServerResponse.status(201).bodyValue(response)))
                .orElse(Mono.error(new IllegalArgumentException("missing user id")));
    }

    public Mono<ServerResponse> getReviews(ServerRequest request) {
        String username = request.queryParam("username").orElse("");
        if (username.isBlank()) {
            return Mono.error(new IllegalArgumentException("missing username parameter"));
        }
        return ServerResponse.ok().body(reviewService.getReviewsForUser(username), com.shop.dto.response.ReviewResponse.class);
    }
}
