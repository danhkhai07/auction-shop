package com.shop.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    public boolean isValidPassword(String password) {
        return true;
    }

    public boolean isValidUsername(String username) {
        return true;
    }
}
