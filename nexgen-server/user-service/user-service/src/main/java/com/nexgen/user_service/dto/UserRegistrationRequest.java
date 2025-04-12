package com.nexgen.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRegistrationRequest {
    private String username;
    private String password;
    private String email;
    private String role;
}
