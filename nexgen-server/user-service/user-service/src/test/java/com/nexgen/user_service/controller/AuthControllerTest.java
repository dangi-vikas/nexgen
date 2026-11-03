package com.nexgen.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.user_service.dto.AuthRequest;
import com.nexgen.user_service.dto.RefreshTokenRequest;
import com.nexgen.user_service.entity.User;
import com.nexgen.user_service.exception.GlobalExceptionHandler;
import com.nexgen.user_service.service.JwtService;
import com.nexgen.user_service.service.KafkaProducerService;
import com.nexgen.user_service.service.LogoutService;
import com.nexgen.user_service.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private LogoutService logoutService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User user() {
        return User.builder()
                .username("john")
                .email("john@example.com")
                .password("encoded")
                .role("ROLE_USER")
                .build();
    }

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void loginReturnsBothTokens() throws Exception {
        User user = user();
        when(userDetailsService.loadUserByUsername("john")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("john", "StrongPass!123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));

        verify(redisService).saveRefreshToken("john", "refresh-token");
    }

    @Test
    void loginRejectsBadCredentials() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("john", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refreshRotatesOneTimeToken() throws Exception {
        User user = user();
        when(jwtService.isRefreshToken("old-refresh")).thenReturn(true);
        when(jwtService.extractUsername("old-refresh")).thenReturn("john");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");
        when(redisService.rotateRefreshToken("john", "old-refresh", "new-refresh"))
                .thenReturn(RedisService.RotationResult.SUCCESS);

        mockMvc.perform(post("/api/v1/users/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("old-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void accessTokenCannotBeUsedToRefresh() throws Exception {
        when(jwtService.isRefreshToken("access-token")).thenReturn(false);

        mockMvc.perform(post("/api/v1/users/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("access-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void replayingRefreshTokenFailsAndRevokesSession() throws Exception {
        when(jwtService.isRefreshToken("stolen")).thenReturn(true);
        when(jwtService.extractUsername("stolen")).thenReturn("john");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(user());
        when(jwtService.generateToken(any())).thenReturn("new-access");
        when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh");
        when(redisService.rotateRefreshToken("john", "stolen", "new-refresh"))
                .thenReturn(RedisService.RotationResult.REUSE_DETECTED);

        mockMvc.perform(post("/api/v1/users/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("stolen"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));

        // The Lua script revokes the session on reuse; the controller must not
        // issue a second delete that could race with a legitimate refresh.
        verify(redisService, never()).deleteRefreshToken(anyString());
    }

    @Test
    void unknownRefreshTokenDoesNotDeleteAValidSession() throws Exception {
        when(jwtService.isRefreshToken("random")).thenReturn(true);
        when(jwtService.extractUsername("random")).thenReturn("john");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(user());
        when(jwtService.generateToken(any())).thenReturn("new-access");
        when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh");
        when(redisService.rotateRefreshToken("john", "random", "new-refresh"))
                .thenReturn(RedisService.RotationResult.INVALID);

        mockMvc.perform(post("/api/v1/users/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("random"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));

        verify(redisService, never()).deleteRefreshToken(anyString());
    }

    @Test
    void refreshRequiresNonBlankToken() throws Exception {
        mockMvc.perform(post("/api/v1/users/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.refreshToken").exists());
    }

    @Test
    void logoutRevokesAccessAndRefreshState() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "john", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(jwtService.extractExpiration("access-token"))
                .thenReturn(new Date(System.currentTimeMillis() + 60_000));

        mockMvc.perform(post("/api/v1/users/logout")
                        .principal(authentication)
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));

        verify(logoutService).blacklistToken(eq("access-token"), anyLong());
        verify(redisService).deleteRefreshToken("john");
    }
}
