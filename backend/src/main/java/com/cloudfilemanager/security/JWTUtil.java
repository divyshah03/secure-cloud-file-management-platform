package com.cloudfilemanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
public class JWTUtil {

    private final String secretKey;
    private final String issuer;
    private final Long expirationDays;

    public JWTUtil(
            @Value("${app.jwt.secret-key:#{T(java.util.UUID).randomUUID().toString().replace('-','').repeat(2)}}") String secretKey,
            @Value("${app.jwt.issuer:FileManagementSystem}") String issuer,
            @Value("${app.jwt.expiration-days:15}") Long expirationDays) {
        this.secretKey = secretKey;
        this.issuer = issuer;
        this.expirationDays = expirationDays;
    }

    public String issueToken(String subject) {
        return issueToken(subject, Map.of());
    }

    public String issueToken(String subject, String... scopes) {
        return issueToken(subject, Map.of("scopes", scopes));
    }

    public String issueToken(String subject, List<String> scopes) {
        return issueToken(subject, Map.of("scopes", scopes));
    }

    public String issueToken(String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(expirationDays, DAYS)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getSubject(String token) {
        return getClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getScopes(String token) {
        Claims claims = getClaims(token);
        Object scopes = claims.get("scopes");
        if (scopes instanceof List) {
            return (List<String>) scopes;
        }
        return List.of();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public boolean isTokenValid(String jwt, String username) {
        try {
            String subject = getSubject(jwt);
            return subject.equals(username) && !isTokenExpired(jwt);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String jwt) {
        Date today = Date.from(Instant.now());
        return getClaims(jwt).getExpiration().before(today);
    }
}
