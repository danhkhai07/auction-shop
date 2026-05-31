package com.frontendauction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontendauction.model.ReviewModel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
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
        String token = TokenStore.getToken();
        if (token == null) {
            return CompletableFuture.failedFuture(new RuntimeException("Unauthorized"));
        }

        try {
            String requestBody = String.format("""
                    {
                        "targetUsername": "%s",
                        "stars": %d,
                        "comment": "%s"
                    }
                    """, targetUsername, stars, comment.replace("\"", "\\\"").replace("\n", "\\n"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
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
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "?username=" + username))
                    .GET()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                return objectMapper.readValue(response.body(), new TypeReference<List<ReviewModel>>() {});
                            } catch (Exception e) {
                                e.printStackTrace();
                                return Collections.emptyList();
                            }
                        }
                        return Collections.emptyList();
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
