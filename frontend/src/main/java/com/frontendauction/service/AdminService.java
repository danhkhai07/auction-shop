package com.frontendauction.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontendauction.model.LiveAuctionModel;
import com.frontendauction.model.UserProfileModel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AdminService {
    private static final String BASE_URL = "http://103.75.182.151:1234";
    private final HttpClient client;
    private final ObjectMapper objectMapper;
    public AdminService() {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private HttpRequest.Builder authorizedRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Accept", "application/json");
        if (TokenStore.hasToken()) {
            builder.header("Authorization", "Bearer " + TokenStore.getToken());
        }
        return builder;
    }

    public CompletableFuture<List<UserProfileModel>> getAllUsers() {
        HttpRequest request = authorizedRequest("/admin/user/all").GET().build();
        return sendListRequest(request, UserProfileModel[].class);
    }

    public CompletableFuture<List<UserProfileModel>> searchUser(String username) {
        return getAllUsers().thenApply(users -> 
            users.stream()
                 .filter(u -> u.getUsername().toLowerCase().contains(username.toLowerCase()))
                 .toList()
        );
    }

    public CompletableFuture<String> deleteUser(String userId) {
        HttpRequest request = authorizedRequest("/admin/delete/user/" + userId)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        return sendActionRequest(request);
    }

    public CompletableFuture<String> banUser(String userId) {
        String requestBody = "{\"reason\":\"Banned by admin\"}";
        HttpRequest request = authorizedRequest("/admin/ban/user/" + userId)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)).build();
        return sendActionRequest(request);
    }

    public CompletableFuture<String> unbanUser(String userId) {
        HttpRequest request = authorizedRequest("/admin/unban/user/" + userId)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        return sendActionRequest(request);
    }

    public CompletableFuture<String> elevateUser(String userId) {
        HttpRequest request = authorizedRequest("/admin/elevate/user/" + userId)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        return sendActionRequest(request);
    }

    public CompletableFuture<List<LiveAuctionModel.AuctionDetail>> getAllAuctions() {
        HttpRequest request = authorizedRequest("/admin/auction/all").GET().build();
        return sendListRequest(request, LiveAuctionModel.AuctionDetail[].class);
    }

    public CompletableFuture<List<LiveAuctionModel.AuctionDetail>> searchAuction(String name) {
        return getAllAuctions().thenApply(auctions -> 
            auctions.stream()
                    .filter(a -> a.getName().toLowerCase().contains(name.toLowerCase()))
                    .toList()
        );
    }

    public CompletableFuture<String> deleteAuction(String auctionId) {
        HttpRequest request = authorizedRequest("/admin/delete/auction/" + auctionId)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        return sendActionRequest(request);
    }

    public CompletableFuture<String> cancelAuction(String auctionId) {
        HttpRequest request = authorizedRequest("/admin/cancel/auction/" + auctionId)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        return sendActionRequest(request);
    }

    private <T> CompletableFuture<List<T>> sendListRequest(HttpRequest request, Class<T[]> clazz) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            T[] array = objectMapper.readValue(response.body(), clazz);
                            return Arrays.asList(array);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.err.println("API error: " + response.statusCode() + " for " + request.uri());
                    }
                    return List.of();
                });
    }

    private CompletableFuture<String> sendActionRequest(HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) return "SUCCESS";
                    try {
                        com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(response.body());
                        if (json.has("message")) return json.get("message").asText();
                        if (json.has("error")) return json.get("error").asText();
                    } catch (Exception e) {}
                    return "Error " + response.statusCode();
                });
    }
}
