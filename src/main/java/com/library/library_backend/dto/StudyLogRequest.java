package com.library.library_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudyLogRequest {
    private String logDate;

    @NotBlank
    @Size(max = 128)
    private String subject;

    @Min(1)
    @Max(24)
    private double hoursStudied;

    @Size(max = 2000)
    private String notes;
}
