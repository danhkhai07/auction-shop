package com.shop.application;

import com.shop.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserManager {
    private final UserRepository userRepository;
    // To be used with cache
    Map<String, User> userIDMap; // id -> user
    Map<String, User> userNameMap; // name -> user

    public Mono<User> getUserByID(String id){
        return userRepository.getByID(id);
    }

    public Mono<User> getUserByName(String name){
        return userRepository.getByName(name);
    }
}
