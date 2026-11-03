package com.nexgen.user_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    public static final String TOKEN_TYPE_CLAIM = "token_type";
    public static final String ROLES_CLAIM = "roles";
    public static final String ACCESS_TOKEN = "access";
    public static final String REFRESH_TOKEN = "refresh";

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final String issuer;
    private final String audience;
    private final long accessTtlMs;
    private final long refreshTtlMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer:nexgen-user-service}") String issuer,
            @Value("${jwt.audience:nexgen-api}") String audience,
            @Value("${jwt.access-ttl-ms:900000}") long accessTtlMs,
            @Value("${jwt.refresh-ttl-ms:604800000}") long refreshTtlMs) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("jwt.issuer must be configured");
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalStateException("jwt.audience must be configured");
        }
        if (accessTtlMs <= 0 || refreshTtlMs <= 0) {
            throw new IllegalStateException("JWT token lifetimes must be positive");
        }
        if (accessTtlMs >= refreshTtlMs) {
            throw new IllegalStateException(
                    "Access token lifetime must be shorter than refresh token lifetime");
        }

        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = issuer;
        this.audience = audience;
        this.accessTtlMs = accessTtlMs;
        this.refreshTtlMs = refreshTtlMs;
    }

    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails, ACCESS_TOKEN, accessTtlMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, REFRESH_TOKEN, refreshTtlMs);
    }

    private String buildToken(UserDetails userDetails, String tokenType, long ttlMs) {
        Instant now = Instant.now();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userDetails.getUsername())
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(ttlMs)))
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .claim(ROLES_CLAIM, roles)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(parseClaims(token));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    /**
     * A bearer credential is only valid if it is an access token for the given
     * user. Refresh tokens are deliberately rejected here.
     */
    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            return ACCESS_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                    && userDetails.getUsername().equals(claims.getSubject())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Compatibility helper retained for callers that only need "is this an
     * access token for this user?".
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isAccessTokenValid(token, userDetails);
    }

    public boolean isRefreshToken(String token) {
        try {
            return REFRESH_TOKEN.equals(extractTokenType(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessTtlMs() {
        return accessTtlMs;
    }

    public long getRefreshTtlMs() {
        return refreshTtlMs;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
