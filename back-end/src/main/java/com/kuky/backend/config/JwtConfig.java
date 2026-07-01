package com.kuky.backend.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtConfig {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiry-seconds:604800}")
    private long expirySeconds;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID userId, String email, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirySeconds * 1000))
                .signWith(getKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload().get("email", String.class);
    }

    /** Reads the role claim; defaults to USER (no special access) for legacy tokens issued before roles existed. */
    public String extractRole(String token) {
        String role = Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload().get("role", String.class);
        return role != null ? role : "USER";
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(
                Jwts.parser().verifyWith(getKey()).build()
                        .parseSignedClaims(token).getPayload().getSubject()
        );
    }
}
