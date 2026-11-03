package com.nexgen.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.user_service.dto.UpdateProfileRequest;
import com.nexgen.user_service.dto.UserProfileResponse;
import com.nexgen.user_service.dto.UserRegistrationRequest;
import com.nexgen.user_service.exception.GlobalExceptionHandler;
import com.nexgen.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(
                "john", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registrationReturnsProfileWithoutPasswordAndIgnoresClientRole() throws Exception {
        when(userService.registerUser(any(UserRegistrationRequest.class)))
                .thenReturn(new UserProfileResponse(1L, "john", "john@example.com", "ROLE_USER"));

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"john\",\"password\":\"StrongPass!123\","
                                + "\"email\":\"john@example.com\",\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        ArgumentCaptor<UserRegistrationRequest> captor =
                ArgumentCaptor.forClass(UserRegistrationRequest.class);
        verify(userService).registerUser(captor.capture());
        assertEquals("john", captor.getValue().getUsername());
        assertEquals("StrongPass!123", captor.getValue().getPassword());
    }

    @Test
    void registrationRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"john\",\"password\":\"short\","
                                + "\"email\":\"john@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void registrationRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"john\",\"password\":\"StrongPass!123\","
                                + "\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void currentProfileUsesAuthenticatedPrincipal() throws Exception {
        when(userService.getUserProfile("john"))
                .thenReturn(new UserProfileResponse(1L, "john", "john@example.com", "ROLE_USER"));

        mockMvc.perform(get("/api/v1/users/me").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void updateProfileUsesAuthenticatedPrincipal() throws Exception {
        mockMvc.perform(put("/api/v1/users/update")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("new@example.com"))))
                .andExpect(status().isOk());

        verify(userService).updateUserProfile("john", new UpdateProfileRequest("new@example.com"));
    }

    @Test
    void changePasswordRejectsShortNewPassword() throws Exception {
        mockMvc.perform(put("/api/v1/users/change-password")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"old\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());
    }
}
