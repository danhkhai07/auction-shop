package com.shop.security.jwt;

import java.util.Date;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class JWTService {
    private final String issuer;
    private final SecretKey SECRET;
    private final long expirationMillis;

    public JWTService(
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms}") long expirationMs
    ) {
        this.SECRET = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
        this.expirationMillis = expirationMs;
    }

    public String extractUserId(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    public String generateToken(String uid) {
        return Jwts.builder()
                .subject(uid)
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(SECRET, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return issuer.equals(claims.getIssuer());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(SECRET)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
