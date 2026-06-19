package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyGoalResponse {
    private int year;
    private int month;
    private int targetMinutes;
    private double targetHours;
    private long completedMinutes;
    private double completedHours;
    private double progressPercent;
}
