package com.shop.application;

import com.shop.domain.User;
import com.shop.dto.request.LoginRequest;
import com.shop.dto.request.RegisterRequest;
import com.shop.dto.response.LoginResponse;
import com.shop.dto.response.RegisterResponse;
import com.shop.security.BCryptHash;
import de.huxhorn.sulky.ulid.ULID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserManager {
    private final UserRepository userRepository;

}
