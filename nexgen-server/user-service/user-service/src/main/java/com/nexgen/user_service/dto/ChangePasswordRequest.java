package com.nexgen.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Size(max = 128, message = "Current password must be at most 128 characters")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 12, max = 128, message = "New password must be between 12 and 128 characters")
    private String newPassword;
}
