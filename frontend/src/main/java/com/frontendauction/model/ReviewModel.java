package com.frontendauction.model;

public class ReviewModel {
    private String reviewer;
    private String targetUser;
    private int stars;
    private String comment;
    private String timestamp;

    public ReviewModel(String reviewer, String targetUser, int stars, String comment) {
        this.reviewer = reviewer;
        this.targetUser = targetUser;
        this.stars = stars;
        this.comment = comment;
        this.timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getReviewer() { return reviewer; }
    public String getTargetUser() { return targetUser; }
    public int getStars() { return stars; }
    public String getComment() { return comment; }
    public String getTimestamp() { return timestamp; }
}
