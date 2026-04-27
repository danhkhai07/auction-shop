package com.shop.application;

import com.shop.domain.User;
import com.shop.dto.request.LoginRequest;
import com.shop.dto.request.RegisterRequest;
import com.shop.dto.response.LoginResponse;
import com.shop.dto.response.RegisterResponse;
import com.shop.security.BCryptHash;
import com.shop.security.jwt.JWTService;
import de.huxhorn.sulky.ulid.ULID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final ULID ulid;

    public boolean isValidPassword(String password) {
        if (password == null) return false;
        if (password.length() < 10) return false;
        if (password.contains(" ")) return false;
        return true;
    }

    public boolean isValidUsername(String username) {
        return username != null &&
                username.matches("^[a-zA-Z0-9_]{6,20}$");
    }

    public Mono<RegisterResponse> register(RegisterRequest request) {
        User user = new User(ulid.nextULID(), request.username);
        String passwordHash = BCryptHash.hash(request.password);
        return userRepository.getByName(request.username)
                .flatMap(id ->
                        Mono.<RegisterResponse>error(new IllegalAccessException("username already exists")))
                .switchIfEmpty(
                        userRepository.newUser(user, passwordHash)
                                .thenReturn(new RegisterResponse("User created"))
                );
    }

    public Mono<LoginResponse> login(LoginRequest request) {
        String invalidCredentialsMessage = "username or password is invalid";
        return userRepository.getByName(request.username)
                .switchIfEmpty(Mono.error(
                        new IllegalAccessException(invalidCredentialsMessage)
                ))
                .filter(user ->
                        BCryptHash.compareHash(request.password, user.passwordHash)
                )
                .switchIfEmpty(Mono.error(
                        new IllegalAccessException(invalidCredentialsMessage)
                ))
                .map(user ->
                        new LoginResponse(jwtService.generateToken(user.id))
                );

    }
}
