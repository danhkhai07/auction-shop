package com.shop.filter;

import com.shop.application.UserManager;
import com.shop.domain.Role;
import com.shop.security.jwt.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RoleFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {
    private final JWTService jwtService;
    private final UserManager userManager;

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        String auth = request.headers().firstHeader("Authorization");

        if (auth == null || !auth.startsWith("Bearer ")) {
            Set<Role> roles = new HashSet<>(Set.of(Role.GUEST));
            ServerRequest newRequest = ServerRequest.from(request)
                    .attribute("resolved_role", roles)
                    .build();
            next.handle(newRequest);
        }

        String token = auth.substring(7);
        try {
            String userID = jwtService.extractUserId(token);
            return userManager.getUserByID(userID)
                    .flatMap(user -> {
                        Set<Role> roles = user.getRoles();
                        ServerRequest newRequest = ServerRequest.from(request)
                                .attribute("resolved_role", roles)
                                .build();
                        return next.handle(newRequest);
                    });
        } catch (Exception e) {
            Set<Role> roles = new HashSet<>(Set.of(Role.GUEST));
            ServerRequest newRequest = ServerRequest.from(request)
                    .attribute("resolved_role", roles)
                    .build();
            next.handle(newRequest);
        }

        return next.handle(request);
    }
}
