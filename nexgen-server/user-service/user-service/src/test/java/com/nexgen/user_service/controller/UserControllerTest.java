package com.nexgen.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.user_service.dto.*;
import com.nexgen.user_service.entity.User;
import com.nexgen.user_service.repository.UserRepository;
import com.nexgen.user_service.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String secretKey;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        UserDetails userDetails =  User.builder()
                .username("john")
                .email("john@example.com")
                .password(passwordEncoder.encode("oldpass"))
                .role("ROLE_USER")
                .build();

        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails);
    }

    public String generateTestToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", List.of("ROLE_USER"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+ 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    @Test
    void testRegister() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("john", "pass", "john@example.com", "ROLE_USER");

        User user = User.builder()
                .username("john")
                .email("john@example.com")
                .role("ROLE_USER")
                .build();

        when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    void testUpdateProfile() throws Exception {
        UpdateProfileRequest updateRequest = new UpdateProfileRequest("john_new@example.com");

        mockMvc.perform(put("/api/v1/users/update")
                        .header("Authorization", "Bearer " + generateTestToken("john"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Profile updated successfully"));
    }

    @Test
    void testChangePassword() throws Exception {
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest("oldpass", "newpass");

        mockMvc.perform(put("/api/v1/users/change-password")
                        .header("Authorization", "Bearer " + generateTestToken("john"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully"));;
    }

    @Test
    void testGetUserByUsername() throws Exception {
        User user = User.builder()
            .username("john")
            .email("john@example.com")
            .password(passwordEncoder.encode("pass"))
            .role("ROLE_USER")
            .build();

        userRepository.save(user);

        when(userService.getUserByUsername("john")).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + generateTestToken("john")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }
}
