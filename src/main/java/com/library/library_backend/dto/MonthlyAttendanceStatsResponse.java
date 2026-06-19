package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAttendanceStatsResponse {
    private int year;
    private int month;
    private int daysPresent;
    private long totalMinutes;
    private double totalHours;
    private Integer rank;
    private int totalStudents;
}
