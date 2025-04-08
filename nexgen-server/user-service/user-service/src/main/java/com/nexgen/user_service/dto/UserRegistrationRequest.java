package com.nexgen.user_service.dto;

import lombok.Data;

@Data
public class UserRegistrationRequest {
    private String username;
    private String password;
    private String email;
    private String role;
}
