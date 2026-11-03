package com.nexgen.user_service.service;

import com.nexgen.user_service.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String OTHER_SECRET = "fedcba9876543210fedcba9876543210";

    private final UserDetails user = User.builder()
            .username("john")
            .password("encoded")
            .role("ROLE_USER")
            .build();

    private JwtService service() {
        return new JwtService(SECRET, "nexgen-user-service", "nexgen-api", 900_000L, 604_800_000L);
    }

    @Test
    void accessTokenCarriesTypeIssuerAudienceAndShortLifetime() {
        JwtService jwtService = service();
        String token = jwtService.generateToken(user);

        assertEquals("john", jwtService.extractUsername(token));
        assertEquals("access", jwtService.extractTokenType(token));
        assertEquals("nexgen-user-service",
                jwtService.extractClaim(token, claims -> claims.getIssuer()));
        assertEquals("nexgen-api",
                jwtService.extractClaim(token, claims -> claims.getAudience()));
        assertEquals(List.of("ROLE_USER"),
                jwtService.extractClaim(token, claims -> claims.get("roles", List.class)));
        assertTrue(jwtService.isAccessTokenValid(token, user));
        assertFalse(jwtService.isRefreshToken(token));

        long lifetime = jwtService.extractExpiration(token).getTime()
                - jwtService.extractClaim(token, claims -> claims.getIssuedAt()).getTime();
        assertTrue(lifetime >= 890_000 && lifetime <= 910_000,
                "access token should use the configured 15 minute TTL, was " + lifetime);
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        JwtService jwtService = service();
        String refresh = jwtService.generateRefreshToken(user);

        assertEquals("refresh", jwtService.extractTokenType(refresh));
        assertTrue(jwtService.isRefreshToken(refresh));
        assertFalse(jwtService.isAccessTokenValid(refresh, user));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService issuer = service();
        JwtService verifier = new JwtService(OTHER_SECRET, "nexgen-user-service", "nexgen-api",
                900_000L, 604_800_000L);

        String token = issuer.generateToken(user);

        assertFalse(verifier.isAccessTokenValid(token, user));
        assertFalse(verifier.isRefreshToken(token));
    }

    @Test
    void tokenFromAnotherIssuerOrAudienceIsRejected() {
        JwtService issuer = service();
        JwtService wrongIssuer = new JwtService(SECRET, "someone-else", "nexgen-api",
                900_000L, 604_800_000L);
        JwtService wrongAudience = new JwtService(SECRET, "nexgen-user-service", "other-api",
                900_000L, 604_800_000L);

        String token = issuer.generateToken(user);

        assertFalse(wrongIssuer.isAccessTokenValid(token, user));
        assertFalse(wrongAudience.isAccessTokenValid(token, user));
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, "nexgen-user-service", "nexgen-api",
                1L, 60_000L);
        String token = shortLived.generateToken(user);

        Thread.sleep(10);

        assertFalse(shortLived.isAccessTokenValid(token, user));
    }

    @Test
    void weakSecretFailsFast() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService("short", "nexgen-user-service", "nexgen-api",
                        900_000L, 604_800_000L));
    }

    @Test
    void accessLifetimeMustBeShorterThanRefreshLifetime() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService(SECRET, "nexgen-user-service", "nexgen-api",
                        1_000L, 1_000L));
    }

    @Test
    void expirationCanBeReadForLogout() {
        JwtService jwtService = service();
        String token = jwtService.generateToken(user);

        Date expiration = jwtService.extractExpiration(token);

        assertTrue(expiration.after(new Date()));
    }
}
