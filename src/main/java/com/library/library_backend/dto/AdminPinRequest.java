package com.library.library_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminPinRequest {
    @NotBlank
    private String pin;
}
