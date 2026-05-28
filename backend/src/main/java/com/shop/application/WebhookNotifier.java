package com.shop.application;

import com.shop.dto.event.AuctionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class WebhookNotifier {
    private final WebClient webClient = WebClient.builder().build();

    @Value("${event.webhook.url:}")
    private String webhookUrl;

    public Mono<Void> notify(AuctionEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return Mono.empty();
        }

        return webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.empty();
                });
    }
}
