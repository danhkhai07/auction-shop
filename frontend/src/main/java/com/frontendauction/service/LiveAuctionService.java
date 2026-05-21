package com.frontendauction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.frontendauction.model.LiveAuctionModel;
import com.frontendauction.model.BidResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class LiveAuctionService {

    private static final String BASE_URL = "http://103.75.182.151:1234";
    private final HttpClient client;
    private final ObjectMapper objectMapper;

    public LiveAuctionService() {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CompletableFuture<LiveAuctionModel.AuctionDetail> getAuctionDetails(String auctionId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auction/" + auctionId))
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            return objectMapper.readValue(response.body(), LiveAuctionModel.AuctionDetail.class);
                        } else {
                            System.err.println("API Error getAuctionDetails: " + response.statusCode()
                                    + " — " + response.body());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                });
    }

    public CompletableFuture<BidResult> placeBid(String auctionId, LiveAuctionModel.BidRequest bidRequest) {
        if (!TokenStore.hasToken()) {
            return CompletableFuture.completedFuture(
                    BidResult.failure("You must login before placing a bid."));
        }

        try {
            String jsonRequest = objectMapper.writeValueAsString(bidRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auction/" + auctionId + "/bid"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + TokenStore.getToken())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(this::mapBidResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(BidResult.failure("Can't create bid request"));
        }
    }

    public CompletableFuture<java.util.List<LiveAuctionModel.AuctionDetail>> getActiveAuctions() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/feed"))
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            LiveAuctionModel.AuctionDetail[] auctions = objectMapper.readValue(
                                    response.body(), LiveAuctionModel.AuctionDetail[].class);
                            return java.util.Arrays.asList(auctions);
                        } else {
                            System.err.println("API Error getActiveAuctions: " + response.statusCode());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return java.util.List.of();
                });
    }

    private BidResult mapBidResponse(HttpResponse<String> response) {
        try {
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return BidResult.ok();
            }

            JsonNode json = objectMapper.readTree(response.body());
            return BidResult.failure(extractErrorMessage(json, "Bid failed!"));
        } catch (Exception exception) {
            return BidResult.failure("Response from server invalid");
        }
    }

    private String extractErrorMessage(JsonNode json, String fallback) {
        String message = json.path("message").asText("");
        if (!message.isBlank()) return message;

        String error = json.path("error").asText("");
        if (!error.isBlank()) return error;

        return fallback;
    }
}
