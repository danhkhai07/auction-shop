package com.shop.application;

import com.shop.domain.Review;
import com.shop.dto.request.ReviewRequest;
import com.shop.dto.response.ReviewResponse;
import de.huxhorn.sulky.ulid.ULID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserManager userManager;
    private final ULID ulid = new ULID();

    public Mono<ReviewResponse> createReview(String reviewerId, ReviewRequest request) {
        return userManager.getUserByID(reviewerId)
                .flatMap(reviewer -> {
                    Review review = Review.builder()
                            .id(ulid.nextULID())
                            .reviewerUsername(reviewer.getUsername())
                            .targetUsername(request.targetUsername())
                            .stars(request.stars())
                            .comment(request.comment())
                            .createdAt(Instant.now())
                            .build();

                    return reviewRepository.save(review)
                            .map(this::mapToResponse);
                });
    }

    public Flux<ReviewResponse> getReviewsForUser(String username) {
        return reviewRepository.findByTargetUsernameOrReviewerUsername(username, username)
                .map(this::mapToResponse);
    }

    private ReviewResponse mapToResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getReviewerUsername(),
                review.getTargetUsername(),
                review.getStars(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
