package com.shop.application;

import com.shop.domain.Review;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ReviewRepository extends ReactiveCrudRepository<Review, String> {
    Flux<Review> findByTargetUsernameOrReviewerUsername(String targetUsername, String reviewerUsername);
}
