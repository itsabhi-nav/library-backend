package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long   id;
    private String memberId;
    private String fullName;
    private String role;
    private String phoneNumber;
}
