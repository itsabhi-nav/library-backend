package com.library.library_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MonthlyGoalRequest {
    private Integer year;
    private Integer month;

    @NotNull
    @Min(1)
    @Max(500)
    private Integer targetHours;
}
