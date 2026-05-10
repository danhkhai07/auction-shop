package com.frontendauction.service;

import com.frontendauction.model.LoginResult;
import com.frontendauction.model.SignupResult;

import java.util.concurrent.CompletableFuture;

public interface AuthService {

    CompletableFuture<LoginResult> login(String username, String password);

    CompletableFuture<SignupResult> signup(String username, String password);
}
