package com.shop.security.jwt;

import java.security.NoSuchAlgorithmException;
import java.util.Date;

import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;

import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

@Service
public class JWTService {
    private final SecretKey SECRET;

    public JWTService() {
        KeyGenerator keyGen;
        try {
             keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyGen.init(256);
        SECRET = keyGen.generateKey();
    }

    public String extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(SECRET)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public String generateToken(String uid) {
        return Jwts.builder()
                .subject(uid)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(SECRET, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(SECRET)
                    .build()
                    .parseSignedClaims(token);
            return "expected-issuer".equals(claims.getPayload().getIssuer());
        } catch (Exception e) {
            return false;
        }
    }
}
