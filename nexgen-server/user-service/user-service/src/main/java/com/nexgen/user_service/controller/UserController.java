package com.nexgen.user_service.controller;

import com.nexgen.user_service.dto.UserRegistrationRequest;
import com.nexgen.user_service.entity.User;
import com.nexgen.user_service.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin()
@Tag(name = "User Controller", description = "Register endpoints")
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody UserRegistrationRequest request) {
        return ResponseEntity.ok(userService.registerUser(request));
    }
}
