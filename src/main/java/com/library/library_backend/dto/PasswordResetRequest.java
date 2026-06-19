package com.library.library_backend.dto;

import lombok.Data;

/** Admin-only request to reset a member's password. */
@Data
public class PasswordResetRequest {
    private String newPassword;
}
