package com.frontendauction.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontendauction.model.ProductManagementModel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ProductManagementService {

    private static final String BASE_URL = "http://103.75.182.151:1234";

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final UserProfileService userProfileService;

    public ProductManagementService() {
        this(HttpClient.newHttpClient(),
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
                new UserProfileService());
    }

    public ProductManagementService(HttpClient client, ObjectMapper objectMapper, UserProfileService userProfileService) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.userProfileService = userProfileService;
    }

    public CompletableFuture<List<ProductManagementModel>> getAllProducts() {
        return userProfileService.getOwnedItems();
    }

    /**
     * Thêm sản phẩm mới. Trả về Optional<String> chứa ID của item vừa tạo nếu thành công,
     * hoặc Optional.empty() nếu thất bại.
     */
    public CompletableFuture<Optional<String>> addProduct(ProductManagementModel product) {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return userProfileService.getCurrentUser()
                .thenCompose(user -> sendItemRequestForId(
                        "/item",
                        userProfileService.createItemPayload(product, user.getId())
                ));
    }

    public CompletableFuture<Boolean> updateProduct(String id, ProductManagementModel product) {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.completedFuture(false);
        }

        return userProfileService.getCurrentUser()
                .thenCompose(user -> sendItemRequest(
                        "/item/" + id,
                        userProfileService.createItemPayload(product, user.getId())
                ));
    }

    public CompletableFuture<Boolean> deleteProduct(String id) {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.completedFuture(false);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/item/delete/" + id))
                .header("Authorization", "Bearer " + TokenStore.getToken())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> response.statusCode() == 200 || response.statusCode() == 201);
    }

    private CompletableFuture<Boolean> sendItemRequest(String path, Map<String, String> payload) {
        try {
            String jsonRequest = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + TokenStore.getToken())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> response.statusCode() == 200 || response.statusCode() == 201);
        } catch (Exception exception) {
            exception.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Gửi POST request và trả về ID từ server response {"id": "..."} nếu thành công.
     */
    private CompletableFuture<Optional<String>> sendItemRequestForId(String path, Map<String, String> payload) {
        try {
            String jsonRequest = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + TokenStore.getToken())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            try {
                                String id = objectMapper.readTree(response.body()).path("id").asText("");
                                return id.isBlank() ? Optional.<String>empty() : Optional.of(id);
                            } catch (Exception e) {
                                System.err.println("[ProductService] Failed to parse ID from response: " + e.getMessage());
                                return Optional.<String>empty();
                            }
                        }
                        System.err.println("[ProductService] POST " + path + " failed with status " + response.statusCode() + ": " + response.body());
                        return Optional.<String>empty();
                    });
        } catch (Exception exception) {
            exception.printStackTrace();
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Tạo auction mới. Body: {"itemID": "...", "startingPrice": ..., "startTime": "...", "endTime": "..."}
     * Endpoint: POST /auction (requires auth)
     */
    public CompletableFuture<Optional<String>> createAuction(String itemId, double startingPrice,
                                                              String startTime, String endTime) {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        try {
            Map<String, Object> payload = Map.of(
                    "itemID", itemId,
                    "startingPrice", startingPrice,
                    "startTime", startTime,
                    "endTime", endTime
            );
            String jsonRequest = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auction"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + TokenStore.getToken())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            try {
                                String id = objectMapper.readTree(response.body()).path("id").asText("");
                                return id.isBlank() ? Optional.<String>empty() : Optional.of(id);
                            } catch (Exception e) {
                                System.err.println("[ProductService] Failed to parse auction ID: " + e.getMessage());
                                return Optional.<String>empty();
                            }
                        }
                        System.err.println("[ProductService] POST /auction failed: " + response.statusCode() + ": " + response.body());
                        return Optional.<String>empty();
                    });
        } catch (Exception exception) {
            exception.printStackTrace();
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }
}
