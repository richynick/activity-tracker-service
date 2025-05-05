package com.richard.activitytracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            throw new TokenExpiredException(
                "Token has expired",
                e.getClaims().getExpiration(),
                new Date(),
                jwtExpiration
            );
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            throw new RuntimeException("Invalid token", e);
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            throw new TokenExpiredException(
                "Token has expired",
                e.getClaims().getExpiration(),
                new Date(),
                jwtExpiration
            );
        } catch (Exception e) {
            log.error("Error extracting claim from token: {}", e.getMessage());
            throw new RuntimeException("Invalid token", e);
        }
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        try {
            Date now = new Date();
            Date expiration = new Date(now.getTime() + jwtExpiration);
            
            log.info("Generating new JWT token for user: {}", userDetails.getUsername());
            log.info("Token issued at: {}", now);
            log.info("Token will expire at: {}", expiration);
            log.info("Token duration: {} milliseconds ({} hours)", jwtExpiration, jwtExpiration / (1000 * 60 * 60));
            
            return Jwts
                    .builder()
                    .setClaims(extraClaims)
                    .setSubject(userDetails.getUsername())
                    .setIssuedAt(now)
                    .setExpiration(expiration)
                    .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("Error generating token: {}", e.getMessage());
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean isValid = (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
            if (!isValid) {
                log.warn("Token validation failed for user: {}", userDetails.getUsername());
            } else {
                log.info("Token is valid for user: {}", userDetails.getUsername());
            }
            return isValid;
        } catch (TokenExpiredException e) {
            log.warn("Token validation failed - token expired: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            Date now = new Date();
            boolean isExpired = expiration.before(now);
            
            if (isExpired) {
                log.warn("Token expired at: {}", expiration);
                log.warn("Current time: {}", now);
                log.warn("Time difference: {} milliseconds", now.getTime() - expiration.getTime());
            } else {
                log.info("Token is still valid. Expires at: {}", expiration);
                log.info("Time remaining: {} milliseconds", expiration.getTime() - now.getTime());
            }
            
            return isExpired;
        } catch (TokenExpiredException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            throw new TokenExpiredException(
                "Token has expired",
                e.getClaims().getExpiration(),
                new Date(),
                jwtExpiration
            );
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            throw new RuntimeException("Unsupported JWT token", e);
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT signature", e);
        } catch (Exception e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw new RuntimeException("Error parsing JWT token", e);
        }
    }

    private Key getSignInKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Error creating signing key: {}", e.getMessage());
            throw new RuntimeException("Error creating signing key", e);
        }
    }
} 