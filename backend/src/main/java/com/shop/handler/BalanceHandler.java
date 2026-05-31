package com.shop.handler;

import com.shop.application.UserManager;
import com.shop.dto.request.DepositRequest;
import com.shop.dto.request.WithdrawRequest;
import com.shop.dto.response.BalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BalanceHandler {

    private final UserManager userManager;

    public Mono<ServerResponse> getBalance(ServerRequest request) {
        String userID = request.attributes().get("userID").toString();
        
        return userManager.getUserByID(userID)
                .map(user -> new BalanceResponse(user.getId(), user.getUsername(), user.getBalance()))
                .flatMap(response -> ServerResponse.status(200).bodyValue(response));
    }

    public Mono<ServerResponse> deposit(ServerRequest request) {
        String userID = request.attributes().get("userID").toString();
        
        return request.bodyToMono(DepositRequest.class)
                .filter(req -> !req.hasEmptyFields())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid deposit amount")))
                .flatMap(depositRequest ->
                        userManager.addBalance(userID, depositRequest.amount())
                                .then(userManager.getUserByID(userID))
                                .map(user -> new BalanceResponse(
                                        user.getId(),
                                        user.getUsername(),
                                        user.getBalance(),
                                        "Deposit successful"
                                ))
                )
                .flatMap(response -> ServerResponse.status(200).bodyValue(response));
    }

    public Mono<ServerResponse> withdraw(ServerRequest request) {
        String userID = request.attributes().get("userID").toString();
        
        return request.bodyToMono(WithdrawRequest.class)
                .filter(req -> !req.hasEmptyFields())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid withdrawal amount")))
                .flatMap(withdrawRequest ->
                        userManager.deductBalance(userID, withdrawRequest.amount())
                                .then(userManager.getUserByID(userID))
                                .map(user -> new BalanceResponse(
                                        user.getId(),
                                        user.getUsername(),
                                        user.getBalance(),
                                        "Withdrawal successful"
                                ))
                                .onErrorResume(IllegalArgumentException.class, Mono::error)
                )
                .flatMap(response -> ServerResponse.status(200).bodyValue(response));
    }
}
