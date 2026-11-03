package com.nexgen.user_service.controller;

import com.nexgen.user_service.dto.ChangePasswordRequest;
import com.nexgen.user_service.dto.UpdateProfileRequest;
import com.nexgen.user_service.dto.UserProfileResponse;
import com.nexgen.user_service.dto.UserRegistrationRequest;
import com.nexgen.user_service.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "Registration and profile endpoints")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        return ResponseEntity.ok(userService.registerUser(request));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<String> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                 Principal principal) {
        userService.updateUserProfile(principal.getName(), request);
        return ResponseEntity.ok("Profile updated successfully");
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                  Principal principal) {
        userService.changeUserPassword(principal.getName(), request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserProfile(authentication.getName()));
    }
}
