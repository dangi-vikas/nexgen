package com.nexgen.user_service.controller;

import com.nexgen.user_service.dto.AuthRequest;
import com.nexgen.user_service.dto.AuthResponse;
import com.nexgen.user_service.dto.RefreshTokenRequest;
import com.nexgen.user_service.dto.UserLoginEvent;
import com.nexgen.user_service.entity.User;
import com.nexgen.user_service.exception.InvalidCredentialsException;
import com.nexgen.user_service.exception.InvalidTokenException;
import com.nexgen.user_service.service.JwtService;
import com.nexgen.user_service.service.KafkaProducerService;
import com.nexgen.user_service.service.LogoutService;
import com.nexgen.user_service.service.RedisService;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Authentication Controller", description = "Login, refresh and logout endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final LogoutService logoutService;
    private final KafkaProducerService kafkaProducerService;
    private final RedisService redisService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request,
                                              HttpServletRequest servletRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        redisService.saveRefreshToken(user.getUsername(), refreshToken);
        kafkaProducerService.sendLoginEvent(new UserLoginEvent(
                request.getUsername(),
                servletRequest.getRemoteAddr(),
                Instant.now()));

        return ResponseEntity.ok(toAuthResponse(user, accessToken, refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || authentication.getName() == null) {
            throw new InvalidTokenException("Authentication is required");
        }

        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                long remaining = jwtService.extractExpiration(token).getTime() - System.currentTimeMillis();
                logoutService.blacklistToken(token, remaining);
            } catch (JwtException | IllegalArgumentException e) {
                // The filter normally rejects this before the controller. Keep
                // logout idempotent for a malformed Authorization header.
            }
        }

        // Logout always revokes the refresh session for the authenticated user.
        redisService.deleteRefreshToken(authentication.getName());
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String presentedToken = request.getRefreshToken();

        if (!jwtService.isRefreshToken(presentedToken)) {
            throw new InvalidTokenException("A valid refresh token is required");
        }

        String username;
        UserDetails userDetails;
        try {
            username = jwtService.extractUsername(presentedToken);
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (JwtException | IllegalArgumentException | org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            throw new InvalidTokenException("A valid refresh token is required");
        }

        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        // Atomic one-time rotation. A replay revokes the current session inside
        // the Lua script; an unknown token is rejected without touching it.
        RedisService.RotationResult rotation =
                redisService.rotateRefreshToken(username, presentedToken, newRefreshToken);
        if (rotation != RedisService.RotationResult.SUCCESS) {
            throw new InvalidTokenException("Refresh token is invalid or has already been used");
        }

        return ResponseEntity.ok(toAuthResponse(userDetails, newAccessToken, newRefreshToken));
    }

    private AuthResponse toAuthResponse(UserDetails userDetails, String accessToken, String refreshToken) {
        String email = userDetails instanceof User user ? user.getEmail() : null;
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);

        return new AuthResponse(
                accessToken,
                refreshToken,
                userDetails.getUsername(),
                email,
                role);
    }
}
