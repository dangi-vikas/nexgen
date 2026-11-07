package com.nexgen.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for an administrator changing another user's role. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateRequest {

    // The whitelist is enforced here as well as in the service, so an unknown role
    // (or a client-invented authority such as ROLE_INTERNAL) is a 400, not a new role.
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ROLE_(USER|ADMIN)", message = "Role must be ROLE_USER or ROLE_ADMIN")
    private String role;
}
