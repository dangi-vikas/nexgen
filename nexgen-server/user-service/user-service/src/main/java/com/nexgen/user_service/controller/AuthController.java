package com.nexgen.user_service.controller;

import com.nexgen.user_service.dto.AuthRequest;
import com.nexgen.user_service.dto.AuthResponse;
import com.nexgen.user_service.service.JwtService;
import com.nexgen.user_service.service.LogoutService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin()
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final LogoutService logoutService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(user);
        String role = ((com.nexgen.user_service.entity.User) user).getRole();
        String email = ((com.nexgen.user_service.entity.User) user).getEmail();

        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), email, role));
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
}
