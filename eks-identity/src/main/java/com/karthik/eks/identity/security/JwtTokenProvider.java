package com.karthik.eks.identity.security;

import com.karthik.eks.identity.entity.User;
import com.karthik.eks.common.exception.ResourceNotFoundException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token Provider for authentication.
 * 
 * Responsibilities:
 * - Generate JWT tokens for authenticated users
 * - Validate and extract claims from JWT tokens
 * - Manage token expiration
 * 
 * JWT Structure: Header.Payload.Signature
 * 
 * The token contains:
 * - userId: The authenticated user's ID
 * - username: The username
 * - role: User's role
 * - exp: Token expiration time
 * - iat: Token issued at time
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:MyVerySecureSecretKeyForJWTTokenGenerationAndValidation123456789}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")  // 24 hours in milliseconds
    private long jwtExpirationMs;

    /**
     * Generate JWT token for a user.
     * 
     * @param user the authenticated user
     * @return JWT token string
     */
    public String generateToken(User user) {
        return generateToken(user, null);
    }

    /**
     * Generate JWT token for a user with optional tenantId.
     * 
     * @param user the authenticated user
     * @param tenantId optional tenant ID to include in token
     * @return JWT token string
     */
    public String generateToken(User user, Long tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().toString());
        
        if (tenantId != null) {
            claims.put("tenantId", tenantId);
        }
        
        return createToken(claims, user.getUsername());
    }

    /**
     * Create the actual JWT token.
     * 
     * @param claims the token claims
     * @param subject the username (subject)
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extract username from JWT token.
     * 
     * @param token the JWT token
     * @return the username
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * Extract user ID from JWT token.
     * 
     * @param token the JWT token
     * @return the user ID
     */
    public Long getUserIdFromToken(String token) {
        return ((Number) getClaimsFromToken(token).get("userId")).longValue();
    }

    /**
     * Extract tenant ID from JWT token.
     * Returns null if tenantId is not present in the token.
     * 
     * @param token the JWT token
     * @return the tenant ID or null
     */
    public Long getTenantIdFromToken(String token) {
        Object tenantId = getClaimsFromToken(token).get("tenantId");
        if (tenantId != null && tenantId instanceof Number) {
            return ((Number) tenantId).longValue();
        }
        return null;
    }

    /**
     * Extract user role from JWT token.
     * 
     * @param token the JWT token
     * @return the user role
     */
    public String getRoleFromToken(String token) {
        return (String) getClaimsFromToken(token).get("role");
    }

    /**
     * Validate JWT token.
     * 
     * @param token the JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract all claims from JWT token.
     * 
     * @param token the JWT token
     * @return the claims
     */
    private Claims getClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new ResourceNotFoundException("Invalid or expired token");
        }
    }
}
