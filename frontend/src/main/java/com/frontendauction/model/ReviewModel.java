package com.frontendauction.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewModel {
    private String id;
    private String reviewerUsername;
    private String targetUsername;
    private int stars;
    private String comment;
    private String createdAt;

    // No-arg constructor for Jackson
    public ReviewModel() {}

    public ReviewModel(String reviewerUsername, String targetUsername, int stars, String comment) {
        this.reviewerUsername = reviewerUsername;
        this.targetUsername = targetUsername;
        this.stars = stars;
        this.comment = comment;
        this.createdAt = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getId() { return id; }
    public String getReviewerUsername() { return reviewerUsername; }
    public String getTargetUsername() { return targetUsername; }
    public int getStars() { return stars; }
    public String getComment() { return comment; }
    public String getCreatedAt() { return createdAt; }

    // Keep old getters as aliases for backward compatibility
    public String getReviewer() { return reviewerUsername; }
    public String getTargetUser() { return targetUsername; }
    public String getTimestamp() { return createdAt; }
}
