package com.nexgen.user_service.controller;

import com.nexgen.user_service.dto.AuthRequest;
import com.nexgen.user_service.dto.AuthResponse;
import com.nexgen.user_service.dto.RefreshTokenRequest;
import com.nexgen.user_service.dto.UserLoginEvent;
import com.nexgen.user_service.entity.User;
import com.nexgen.user_service.exception.InvalidCredentialsException;
import com.nexgen.user_service.service.JwtService;
import com.nexgen.user_service.service.KafkaProducerService;
import com.nexgen.user_service.service.LogoutService;
import com.nexgen.user_service.service.RedisService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin()
@Tag(name = "Authentication Controller", description = "Login and Logout endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final LogoutService logoutService;
    private final KafkaProducerService kafkaProducerService;
    private final RedisService redisService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request, HttpServletRequest servletRequest) {
        try{
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        String role = ((User) user).getRole();
        String email = ((User) user).getEmail();
        String ip = servletRequest.getRemoteAddr();
        UserLoginEvent loginEvent = new UserLoginEvent(
                request.getUsername(),
                ip,
                Instant.now()
        );

        redisService.saveRefreshToken(user.getUsername(), refreshToken);
        kafkaProducerService.sendLoginEvent(loginEvent);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, user.getUsername(), email, role));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            long expiry = jwtService.extractExpiration(token).getTime() - System.currentTimeMillis();
            logoutService.blacklistToken(token, expiry);
        }

        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        String username = jwtService.extractUsername(request.getRefreshToken());
        String storedRefreshToken = redisService.getRefreshToken(username);

        if (storedRefreshToken == null || !storedRefreshToken.equals(request.getRefreshToken())) {
            throw new RuntimeException("Invalid refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        redisService.saveRefreshToken(username, newRefreshToken);

        return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken, userDetails.getUsername(),
                ((User) userDetails).getEmail(), ((User) userDetails).getRole()));
    }

}
