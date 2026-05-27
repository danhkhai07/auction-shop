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
                        userManager.getUserByID(userID)
                                .flatMap(user -> {
                                    user.deposit(depositRequest.amount());
                                    return userManager.updateUser(user)
                                            .thenReturn(new BalanceResponse(
                                                    user.getId(),
                                                    user.getUsername(),
                                                    user.getBalance(),
                                                    "Deposit successful"
                                            ));
                                })
                )
                .flatMap(response -> ServerResponse.status(200).bodyValue(response));
    }

    public Mono<ServerResponse> withdraw(ServerRequest request) {
        String userID = request.attributes().get("userID").toString();
        
        return request.bodyToMono(WithdrawRequest.class)
                .filter(req -> !req.hasEmptyFields())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid withdrawal amount")))
                .flatMap(withdrawRequest ->
                        userManager.getUserByID(userID)
                                .flatMap(user -> {
                                    try {
                                        user.withdraw(withdrawRequest.amount());
                                        return userManager.updateUser(user)
                                                .thenReturn(new BalanceResponse(
                                                        user.getId(),
                                                        user.getUsername(),
                                                        user.getBalance(),
                                                        "Withdrawal successful"
                                                ));
                                    } catch (IllegalArgumentException e) {
                                        return Mono.error(e);
                                    }
                                })
                )
                .flatMap(response -> ServerResponse.status(200).bodyValue(response));
    }
}
