package com.example.iTDS.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key secretKey = Keys.hmacShaKeyFor("mySuperLongSecureSecretKey12345!".getBytes());
    // Secret key for signing the token
    private final long expirationTime = 1000 * 60 * 60; // 1 hour

    // Generate a JWT token
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username) // Username as the subject
                .claim("username", username)
                .claim("role", role) // Adding role as a claim
                .setIssuedAt(new Date()) // Set issue time
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // Set expiration
                .signWith(secretKey, SignatureAlgorithm.HS256) // Sign with the secret key
                .compact();
    }
    public Long extractUserId(String token) {
        try {
            token = removeBearerPrefix(token);
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("userId", Long.class);
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to extract user ID from token: Invalid or malformed token", e);
        }
    }
    private String removeBearerPrefix(String token) {
        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }
    // Extract username (subject) from the token
    public String extractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to extract username from token: Invalid or malformed token", e);
        }
    }

    // Extract role from the token
    public String extractRole(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("role", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to extract role from token: Invalid or malformed token", e);
        }
    }


    // Validate token and ensure it matches the username
    public boolean isTokenValid(String token, String username) {
        try {
            return username.equals(extractUsername(token)) && !isTokenExpired(token);
        } catch (Exception e) {
            return false; // Return false if any exception occurs during validation
        }
    }

    // Check if the token is expired
    private boolean isTokenExpired(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration()
                    .before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to validate token expiration: Invalid or malformed token", e);
        }
    }
}
