package com.frontendauction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontendauction.model.ReviewModel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReviewService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl = "http://103.75.182.151:1234/review";

    public ReviewService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<Void> submitReview(String targetUsername, int stars, String comment) {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.failedFuture(new RuntimeException("Unauthorized"));
        }
        String token = TokenStore.getToken();

        try {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
            payload.put("targetUsername", targetUsername);
            payload.put("stars", stars);
            payload.put("comment", comment == null ? "" : comment);
            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenAccept(response -> {
                        if (response.statusCode() != 201 && response.statusCode() != 200) {
                            throw new RuntimeException("Failed to submit review: " + response.body());
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<List<ReviewModel>> getReviewsForUser(String username) {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.failedFuture(new RuntimeException("Unauthorized"));
        }
        String token = TokenStore.getToken();

        try {
            String encodedUsername = URLEncoder.encode(username == null ? "" : username, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "?username=" + encodedUsername))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                return objectMapper.readValue(response.body(), new TypeReference<List<ReviewModel>>() {});
                            } catch (Exception e) {
                                throw new RuntimeException("Invalid review response", e);
                            }
                        }
                        throw new RuntimeException("Failed to load reviews: " + response.statusCode() + " " + response.body());
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public Double averageRating(List<ReviewModel> reviews, String targetUsername) {
        if (reviews == null || targetUsername == null || targetUsername.isBlank()) {
            return null;
        }

        double sum = 0;
        int count = 0;
        for (ReviewModel review : reviews) {
            if (review == null || review.getTargetUser() == null) {
                continue;
            }
            if (review.getTargetUser().equalsIgnoreCase(targetUsername)) {
                sum += review.getStars();
                count++;
            }
        }

        return count > 0 ? sum / count : null;
    }
}
