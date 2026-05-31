package com.frontendauction.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontendauction.model.LiveAuctionModel;
import com.frontendauction.model.ProductManagementModel;
import com.frontendauction.model.UserProfileModel;
import com.frontendauction.model.BalanceResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class UserProfileService {

    private static final String BASE_URL = "http://103.75.182.151:1234";

    private final HttpClient client;
    private final ObjectMapper objectMapper;

    public UserProfileService() {
        this(HttpClient.newHttpClient(), new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
    }

    public UserProfileService(HttpClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<UserProfileModel> getCurrentUser() {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.failedFuture(new IllegalStateException("You must login first."));
        }

        HttpRequest request = authorizedRequest("/auth/me")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            UserProfileModel user = objectMapper.readValue(response.body(), UserProfileModel.class);
                            return CompletableFuture.completedFuture(user);
                        } catch (Exception exception) {
                            return CompletableFuture.failedFuture(
                                    new IllegalStateException("Invalid profile response", exception)
                            );
                        }
                    }

                    return CompletableFuture.failedFuture(
                            new IllegalStateException(readError(response.body(), "Unable to load current user"))
                    );
                });
    }

    public CompletableFuture<List<ProductManagementModel>> getOwnedItems() {
        return getCurrentUser().thenCompose(this::getOwnedItems);
    }

    public CompletableFuture<List<ProductManagementModel>> getOwnedItems(UserProfileModel user) {
        return sequence(safeList(user.getItemList()), this::getItemById);
    }

    public CompletableFuture<List<LiveAuctionModel.AuctionDetail>> getOwnedAuctions() {
        return getCurrentUser().thenCompose(this::getOwnedAuctions);
    }

    public CompletableFuture<List<LiveAuctionModel.AuctionDetail>> getOwnedAuctions(UserProfileModel user) {
        return sequence(safeList(user.getAuctionList()), this::getAuctionById);
    }

    public CompletableFuture<ProductManagementModel> getItemById(String itemId) {
        HttpRequest request = authorizedRequest("/item/" + itemId)
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode json = objectMapper.readTree(response.body());
                            ProductManagementModel item = new ProductManagementModel();
                            item.setId(json.path("id").asText(""));
                            item.setName(json.path("name").asText(""));
                            item.setDescription(json.path("description").asText(""));
                            item.setSellerId(json.path("sellerID").asText(""));
                            return CompletableFuture.completedFuture(item);
                        } catch (Exception exception) {
                            return CompletableFuture.failedFuture(
                                    new IllegalStateException("Invalid item response", exception)
                            );
                        }
                    }

                    return CompletableFuture.failedFuture(
                            new IllegalStateException(readError(response.body(), "Unable to load item"))
                    );
                });
    }

    public CompletableFuture<LiveAuctionModel.AuctionDetail> getAuctionById(String auctionId) {
        HttpRequest request = plainRequest("/auction/" + auctionId)
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            LiveAuctionModel.AuctionDetail auction = objectMapper.readValue(
                                    response.body(),
                                    LiveAuctionModel.AuctionDetail.class
                            );
                            return CompletableFuture.completedFuture(auction);
                        } catch (Exception exception) {
                            return CompletableFuture.failedFuture(
                                    new IllegalStateException("Invalid auction response", exception)
                            );
                        }
                    }

                    return CompletableFuture.failedFuture(
                            new IllegalStateException(readError(response.body(), "Unable to load auction"))
                    );
                });
    }

    public Map<String, String> createItemPayload(ProductManagementModel product, String sellerId) {
        return Map.of(
                "name", defaultText(product.getName()),
                "description", defaultText(product.getDescription()),
                "sellerID", defaultText(sellerId)
        );
    }

    private HttpRequest.Builder plainRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Accept", "application/json");
    }

    private HttpRequest.Builder authorizedRequest(String path) {
        HttpRequest.Builder builder = plainRequest(path);
        if (TokenStore.hasToken()) {
            builder.header("Authorization", "Bearer " + TokenStore.getToken());
        }
        return builder;
    }

    private String readError(String body, String fallback) {
        try {
            JsonNode json = objectMapper.readTree(body);
            String message = json.path("message").asText("");
            if (!message.isBlank()) {
                return message;
            }
            String error = json.path("error").asText("");
            if (!error.isBlank()) {
                return error;
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private <T> CompletableFuture<List<T>> sequence(List<String> ids, Function<String, CompletableFuture<T>> loader) {
        if (ids.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<CompletableFuture<T>> futures = new ArrayList<>();
        for (String id : ids) {
            futures.add(loader.apply(id).exceptionally(exception -> null));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .toList());
    }

    public CompletableFuture<Void> startAuction(String auctionId) {
        return sendAction(auctionId, "start");
    }

    public CompletableFuture<Void> pauseAuction(String auctionId) {
        return sendAction(auctionId, "pause");
    }

    public CompletableFuture<Void> unpauseAuction(String auctionId) {
        return sendAction(auctionId, "unpause");
    }

    public CompletableFuture<Void> cancelAuction(String auctionId) {
        return sendAction(auctionId, "cancel");
    }

    public CompletableFuture<Void> endAuction(String auctionId) {
        return sendAction(auctionId, "end");
    }

    public CompletableFuture<Void> extendAuctionTime(String auctionId, String newEndTimeStr) {
        try {
            // we assume newEndTimeStr is a valid ISO-8601 string that backend can parse as LocalDateTime
            String payload = "{\"newEndTime\":\"" + newEndTimeStr + "\"}";
            HttpRequest request = authorizedRequest("/auction/" + auctionId + "/extend/endtime")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenCompose(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204) {
                            return CompletableFuture.completedFuture(null);
                        }
                        return CompletableFuture.failedFuture(
                                new IllegalStateException(readError(response.body(), "Unable to extend time"))
                        );
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Void> sendAction(String auctionId, String action) {
        HttpRequest request = authorizedRequest("/auction/" + auctionId + "/" + action)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return CompletableFuture.failedFuture(
                            new IllegalStateException(readError(response.body(), "Unable to " + action + " auction"))
                    );
                });
    }

    public CompletableFuture<BalanceResponse> getBalance() {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.failedFuture(new IllegalStateException("You must login first."));
        }

        HttpRequest request = authorizedRequest("/balance")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            BalanceResponse balanceData = objectMapper.readValue(response.body(), BalanceResponse.class);
                            return CompletableFuture.completedFuture(balanceData);
                        } catch (Exception exception) {
                            return CompletableFuture.failedFuture(
                                    new IllegalStateException("Invalid balance response", exception)
                            );
                        }
                    }
                    return CompletableFuture.failedFuture(
                            new RuntimeException("Failed to get balance. Server returned: " + response.statusCode())
                    );
                });
    }

    public CompletableFuture<BalanceResponse> deposit(double amount) {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.failedFuture(new IllegalStateException("You must login first."));
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(Map.of("amount", amount));
            HttpRequest request = authorizedRequest("/balance/deposit")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .header("Content-Type", "application/json")
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenCompose(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            try {
                                BalanceResponse balanceData = objectMapper.readValue(response.body(), BalanceResponse.class);
                                return CompletableFuture.completedFuture(balanceData);
                            } catch (Exception exception) {
                                return CompletableFuture.failedFuture(
                                        new IllegalStateException("Invalid deposit response", exception)
                                );
                            }
                        }
                        return CompletableFuture.failedFuture(
                                new RuntimeException("Failed to deposit. Server returned: " + response.statusCode() + " " + response.body())
                        );
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<BalanceResponse> withdraw(double amount) {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.failedFuture(new IllegalStateException("You must login first."));
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(Map.of("amount", amount));
            HttpRequest request = authorizedRequest("/balance/withdraw")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .header("Content-Type", "application/json")
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenCompose(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            try {
                                BalanceResponse balanceData = objectMapper.readValue(response.body(), BalanceResponse.class);
                                return CompletableFuture.completedFuture(balanceData);
                            } catch (Exception exception) {
                                return CompletableFuture.failedFuture(
                                        new IllegalStateException("Invalid withdraw response", exception)
                                );
                            }
                        }
                        return CompletableFuture.failedFuture(
                                new RuntimeException("Failed to withdraw. Server returned: " + response.statusCode() + " " + response.body())
                        );
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
