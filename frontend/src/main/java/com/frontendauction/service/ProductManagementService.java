package com.frontendauction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontendauction.model.ProductManagementModel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ProductManagementService {

    private static final String BASE_URL = "http://103.75.182.151:1234";
    private final HttpClient client;
    private final ObjectMapper objectMapper;

    public ProductManagementService() {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CompletableFuture<List<ProductManagementModel>> getAllProducts() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/feed"))
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            ProductManagementModel[] products = objectMapper.readValue(
                                    response.body(), ProductManagementModel[].class);
                            return Arrays.asList(products);
                        } else {
                            System.err.println("API Error getAllProducts: " + response.statusCode());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return List.of();
                });
    }

    public CompletableFuture<Boolean> addProduct(ProductManagementModel product) {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.completedFuture(false);
        }

        try {
            String jsonRequest = objectMapper.writeValueAsString(Map.of(
                    "name", product.getName() != null ? product.getName() : "",
                    "description", product.getDescription() != null ? product.getDescription() : ""
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/item"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + TokenStore.getToken())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> response.statusCode() == 200 || response.statusCode() == 201);
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<Boolean> updateProduct(String id, ProductManagementModel product) {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.completedFuture(false);
        }

        try {
            String jsonRequest = objectMapper.writeValueAsString(Map.of(
                    "name", product.getName() != null ? product.getName() : "",
                    "description", product.getDescription() != null ? product.getDescription() : ""
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/item/" + id))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + TokenStore.getToken())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> response.statusCode() == 200 || response.statusCode() == 201);
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
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
}
