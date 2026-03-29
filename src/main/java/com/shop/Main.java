package com.shop;

import com.shop.app.App;

public class Main {
    public static void main(String[] args) {
        App server = new App();
        server.Run(args);
    }

//    @GetMapping("/")
//    public Flux<Integer> mainpage() {
//        return Flux.range(1, 1230);
//    }
//
//    @GetMapping("/{id}")
//    public Mono<String> iduserbral(@PathVariable int id) {
//        return Mono.just("user " + id);
//    }
//
//    @GetMapping("/search")
//    public Mono<String> searchengine(
//            @RequestParam String word,
//            @RequestParam int key
//    ) {
//        return Mono.just("searching: " + word + " with key: " + key);
//    }
//
//    @GetMapping("/test")
//    public Mono<String> test() {
//        return Mono.just("user")
//                .map(user -> user + " data");
//    }

}
