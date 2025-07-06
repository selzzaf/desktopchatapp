package com.chatrealtime.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Token Provider for handling JSON Web Token operations in the Chat Realtime application.
 * 
 * This component provides functionality for:
 * - Generating JWT tokens for authenticated users
 * - Validating JWT tokens
 * - Extracting user information from tokens
 * - Managing token expiration
 * 
 * The implementation uses HMAC-SHA512 algorithm for token signing and validation.
 * 

 */
@Component
public class JwtTokenProvider {
    
    /**
     * Logger instance for tracking JWT operations and errors
     */
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    /**
     * Secret key used for signing and validating JWT tokens
     */
    private final SecretKey key;
    
    /**
     * Token expiration time in milliseconds
     */
    private final Long expiration;

    /**
     * Constructs a new JWT Token Provider with the specified secret and expiration time.
     * 
     * @param secret The secret key used for JWT signing (configured via application.properties)
     * @param expiration The token expiration time in milliseconds (configured via application.properties)
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") Long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * Generates a new JWT token for the specified user ID.
     * 
     * The token includes:
     * - User ID as the subject
     * - Current timestamp as issued date
     * - Expiration date based on configured expiration time
     * - HMAC-SHA512 signature
     * 
     * @param userId The unique identifier of the user
     * @return A signed JWT token string
     */
    public String generateToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        return createToken(claims, userId);
    }

    /**
     * Creates a JWT token with the specified claims and subject.
     * 
     * @param claims Additional claims to include in the token
     * @param subject The subject (user ID) of the token
     * @return A signed JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

    /**
     * Extracts the user ID from a JWT token.
     * 
     * @param token The JWT token to extract the user ID from
     * @return The user ID as a string
     */
    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a JWT token.
     * 
     * @param token The JWT token to extract the expiration date from
     * @return The expiration date as a Date object
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from a JWT token using a claims resolver function.
     * 
     * @param <T> The type of the claim to extract
     * @param token The JWT token to extract the claim from
     * @param claimsResolver Function to resolve the specific claim from the token claims
     * @return The extracted claim value
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses a JWT token and returns all claims.
     * 
     * @param token The JWT token to parse
     * @return All claims from the token
     * @throws JwtException if the token is invalid or cannot be parsed
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * Checks if a JWT token has expired.
     * 
     * @param token The JWT token to check
     * @return true if the token has expired, false otherwise
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * Validates a JWT token by checking its signature and expiration.
     * 
     * This method performs the following validations:
     * - Verifies the token signature using the secret key
     * - Checks if the token has expired
     * - Ensures the token format is valid
     * 
     * @param token The JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
} 