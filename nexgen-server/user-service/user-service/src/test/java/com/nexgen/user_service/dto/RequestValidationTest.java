package com.nexgen.user_service.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void validRegistrationHasNoViolations() {
        UserRegistrationRequest request =
                new UserRegistrationRequest("john", "StrongPass!123", "john@example.com");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void weakPasswordsAreRejected() {
        UserRegistrationRequest request =
                new UserRegistrationRequest("john", "short", "john@example.com");

        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void invalidEmailAndUsernameAreRejected() {
        UserRegistrationRequest request =
                new UserRegistrationRequest("bad username", "StrongPass!123", "not-an-email");

        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void passwordChangeRequiresAStrongNewPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("old", "short");

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newPassword")));
    }

    @Test
    void loginRequiresCredentials() {
        Set<ConstraintViolation<AuthRequest>> violations =
                validator.validate(new AuthRequest("", ""));

        assertEquals(2, violations.size());
    }

    @Test
    void blankRefreshTokenIsRejected() {
        Set<ConstraintViolation<RefreshTokenRequest>> violations =
                validator.validate(new RefreshTokenRequest(" "));

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken")));
    }

    @Test
    void clientSuppliedRoleIsNotPartOfTheRegistrationContract() throws Exception {
        String json = "{\"username\":\"john\",\"password\":\"StrongPass!123\","
                + "\"email\":\"john@example.com\",\"role\":\"ROLE_ADMIN\"}";

        UserRegistrationRequest request = new ObjectMapper().readValue(json, UserRegistrationRequest.class);

        assertEquals("john", request.getUsername());
        // There is deliberately no role property on the DTO. The service always
        // assigns ROLE_USER, so the request cannot escalate privileges.
        assertFalse(java.util.Arrays.stream(UserRegistrationRequest.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("role")));
    }

    @Test
    void profileSerializationContainsNoPassword() throws Exception {
        String json = new ObjectMapper().writeValueAsString(
                new UserProfileResponse(1L, "john", "john@example.com", "ROLE_USER"));

        assertFalse(json.toLowerCase().contains("password"));
    }
}
