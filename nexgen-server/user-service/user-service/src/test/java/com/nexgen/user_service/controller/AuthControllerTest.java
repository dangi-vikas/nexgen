package com.nexgen.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.user_service.dto.AuthRequest;
import com.nexgen.user_service.dto.RefreshTokenRequest;
import com.nexgen.user_service.entity.User;
import com.nexgen.user_service.exception.InvalidCredentialsException;
import com.nexgen.user_service.service.KafkaProducerService;
import com.nexgen.user_service.service.JwtService;
import com.nexgen.user_service.service.LogoutService;
import com.nexgen.user_service.service.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private LogoutService logoutService;

    @MockitoBean
    private KafkaProducerService kafkaProducerService;

    @MockitoBean
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testLoginSuccess() throws Exception {
        AuthRequest request = new AuthRequest("john", "pass");

        User mockUser = User.builder()
                .username("john")
                .email("john@example.com")
                .role("ROLE_USER")
                .password("encodedPass")
                .build();

        when(userDetailsService.loadUserByUsername("john")).thenReturn(mockUser);
        when(jwtService.generateToken(mockUser)).thenReturn("mock-access-token");
        when(jwtService.generateRefreshToken(mockUser)).thenReturn("mock-refresh-token");

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void testLoginFailure() throws Exception {
        AuthRequest request = new AuthRequest("john", "wrongpass");

        doThrow(new InvalidCredentialsException("Invalid username or password")).when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testLogoutSuccess() throws Exception {
        String token = "mock-token";
        Date expiry = new Date(System.currentTimeMillis() + 60000);

        when(jwtService.extractExpiration(token)).thenReturn(expiry);

        mockMvc.perform(post("/api/v1/users/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));

        verify(logoutService).blacklistToken(eq(token), anyLong());
    }

    @Test
    void testRefreshTokenSuccess() throws Exception {
        String oldRefreshToken = "old-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        User user = User.builder()
                .username("john")
                .email("john@example.com")
                .role("ROLE_USER")
                .build();

        when(jwtService.extractUsername(oldRefreshToken)).thenReturn("john");
        when(redisService.getRefreshToken("john")).thenReturn(oldRefreshToken);
        when(userDetailsService.loadUserByUsername("john")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(newAccessToken);
        when(jwtService.generateRefreshToken(user)).thenReturn(newRefreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest(oldRefreshToken);

        mockMvc.perform(post("/api/v1/users/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(newAccessToken))
                .andExpect(jsonPath("$.refreshToken").value(newRefreshToken))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }
}
