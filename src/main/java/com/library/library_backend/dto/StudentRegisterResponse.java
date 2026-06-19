package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Returned after successful student registration, includes the auto-generated memberId. */
@Data
@AllArgsConstructor
public class StudentRegisterResponse {
    private Long   id;
    private String memberId;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String role;
    private String assignedSeatNumber;
    private String defaultPassword; // Shown once so admin can share with student
}
