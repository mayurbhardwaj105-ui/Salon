package com.salon.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Handles creation and verification of JSON Web Tokens (JWT)
 * used to authenticate salon customers.
 */
@Component
public class JwtUtil {

    // In production, load this from an environment variable / secrets manager,
    // never hardcode it. Must be at least 256 bits for HS256.
    @Value("${jwt.secret:this-is-a-demo-secret-key-change-me-1234567890}")
    private String secret;

    @Value("${jwt.expiration-ms:3600000}") // 1 hour default
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generates a signed JWT containing the customer's email as subject.
     */
    public String generateToken(String email, Long customerId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("customerId", customerId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parses and validates the token, throwing JwtException if invalid/expired.
     */
    public Claims validateAndGetClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return validateAndGetClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            validateAndGetClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
