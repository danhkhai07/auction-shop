package com.frontendauction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontendauction.model.LoginResult;
import com.frontendauction.model.SignupResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HttpAuthService implements AuthService {

    private static final String LOGIN_URL = "http://103.75.182.151:1234/auth/login";
    private static final String REGISTER_URL = "http://103.75.182.151:1234/auth/register";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpAuthService() {
        this(HttpClient.newHttpClient(), new ObjectMapper());
    }

    public HttpAuthService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<LoginResult> login(String username, String password) {
        final String body;

        try {
            body = createAuthBody(username, password);
        } catch (Exception exception) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Can't create login request", exception)
            );
        }

        HttpRequest request = buildPostRequest(LOGIN_URL, body);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(this::mapLoginResponse);
    }

    @Override
    public CompletableFuture<SignupResult> signup(String username, String password) {
        final String body;

        try {
            body = createAuthBody(username, password);
        } catch (Exception exception) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Can't create signup request", exception)
            );
        }

        HttpRequest request = buildPostRequest(REGISTER_URL, body);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(this::mapSignupResponse);
    }

    private String createAuthBody(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("username", username, "password", password)
        );
    }

    private HttpRequest buildPostRequest(String url, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
    }

    private LoginResult mapLoginResponse(HttpResponse<String> response) {
        try {
            JsonNode json = objectMapper.readTree(response.body());

            if (response.statusCode() == 201) {
                return LoginResult.success(json.path("token").asText(""));
            }

            return LoginResult.failure(extractErrorMessage(json, "Login failed!"));
        } catch (Exception exception) {
            return LoginResult.failure("Response from server invalid");
        }
    }

    private SignupResult mapSignupResponse(HttpResponse<String> response) {
        try {
            JsonNode json = objectMapper.readTree(response.body());

            if (response.statusCode() == 201) {
                return SignupResult.success(json.path("message").asText("User created"));
            }

            return SignupResult.failure(extractErrorMessage(json, "Signup failed!"));
        } catch (Exception exception) {
            return SignupResult.failure("Response from server invalid");
        }
    }

    private String extractErrorMessage(JsonNode json, String fallback) {
        String message = json.path("message").asText("");

        if (!message.isBlank()) {
            return message;
        }

        String error = json.path("error").asText("");

        if (!error.isBlank()) {
            return error;
        }

        return fallback;
    }
}
