package com.frontendauction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.frontendauction.model.AuctionEventData;
import com.frontendauction.model.LiveAuctionModel;
import com.frontendauction.model.BidResult;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class LiveAuctionService {

    private static final String BASE_URL = "http://103.75.182.151:1234";
    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private volatile Thread sseThread;
    private volatile boolean sseConnected;

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
                .thenCompose(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            return CompletableFuture.completedFuture(
                                    objectMapper.readValue(response.body(), LiveAuctionModel.AuctionDetail.class));
                        }
                        String errMsg = "Server returned status " + response.statusCode();
                        System.err.println("API Error getAuctionDetails: " + response.statusCode() + " — " + response.body());
                        return CompletableFuture.failedFuture(new IllegalStateException(errMsg));
                    } catch (Exception e) {
                        return CompletableFuture.failedFuture(e);
                    }
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

    /**
     * Connect to SSE event stream for real-time auction updates.
     * Events are delivered on a background thread — caller must use Platform.runLater for UI updates.
     */
    public void connectToEventStream(String auctionId, Consumer<AuctionEventData> onEvent, Runnable onError) {
        disconnectEventStream();

        sseConnected = true;
        sseThread = new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/auction/" + auctionId + "/events"))
                        .header("Accept", "text/event-stream")
                        .GET()
                        .build();

                HttpResponse<InputStream> response = client.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    System.err.println("SSE connection failed: status " + response.statusCode());
                    if (onError != null) onError.run();
                    return;
                }

                System.out.println("SSE connected to auction: " + auctionId);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    StringBuilder dataBuffer = new StringBuilder();
                    String line;

                    while (sseConnected && (line = reader.readLine()) != null) {
                        if (Thread.currentThread().isInterrupted()) break;

                        if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            dataBuffer.append(data);
                        } else if (line.isEmpty() && !dataBuffer.isEmpty()) {
                            // Empty line = end of SSE event
                            try {
                                String jsonData = dataBuffer.toString();
                                AuctionEventData event = objectMapper.readValue(
                                        jsonData, AuctionEventData.class);
                                onEvent.accept(event);
                            } catch (Exception e) {
                                System.err.println("Failed to parse SSE event: " + dataBuffer);
                                e.printStackTrace();
                            }
                            dataBuffer.setLength(0);
                        }
                        // Ignore "event:", "id:", "retry:" lines
                    }
                }
            } catch (Exception e) {
                if (sseConnected) {
                    System.err.println("SSE stream error: " + e.getMessage());
                    if (onError != null) onError.run();
                }
            }

            System.out.println("SSE disconnected from auction: " + auctionId);
        }, "SSE-AuctionStream-" + auctionId);

        sseThread.setDaemon(true);
        sseThread.start();
    }

    /**
     * Disconnect from the current SSE stream.
     */
    public void disconnectEventStream() {
        sseConnected = false;
        if (sseThread != null) {
            sseThread.interrupt();
            sseThread = null;
        }
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

