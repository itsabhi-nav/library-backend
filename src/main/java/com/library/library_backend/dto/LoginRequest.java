package com.library.library_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Student/Admin login uses memberId + password (not email). */
@Data
public class LoginRequest {
    @NotBlank(message = "Member ID is required")
    private String memberId;

    @NotBlank(message = "Password is required")
    private String password;
}
