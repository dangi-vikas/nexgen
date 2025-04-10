package com.nexgen.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginEvent {
    private String username;
    private String ipAddress;
    private Instant loginTime;
}
