package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalAnalyticsResponse {
    private int year;
    private int month;
    private long totalMinutesThisMonth;
    private double totalHoursThisMonth;
    private double averageDailyHours;
    private String bestStudyDay;
    private long bestStudyDayMinutes;
    private int currentStreak;
    private int longestStreak;
    private double attendancePercent;
    private int daysPresentThisMonth;
    private int daysElapsedInMonth;
}
