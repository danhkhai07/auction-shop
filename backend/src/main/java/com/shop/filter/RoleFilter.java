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
            request.attributes().put("resolved_role", roles);
            return next.handle(request);
        }

        String token = auth.substring(7);
        try {
            String userID = jwtService.extractUserId(token);
            return userManager.getUserByID(userID)
                    .switchIfEmpty(Mono.error(new IllegalStateException("user does not exist")))
                    .flatMap(user -> {
                        if (user.isBanned()) {
                            return ServerResponse.status(403).build();
                        }
                        Set<Role> roles = user.getRoles();
                        request.attributes().put("resolved_role", roles);
                        request.attributes().put("userID", userID);
                        return next.handle(request);
                    });
        } catch (Exception e) {
            Set<Role> roles = new HashSet<>(Set.of(Role.GUEST));
            request.attributes().put("resolved_role", roles);
            return next.handle(request);
        }
    }
}
