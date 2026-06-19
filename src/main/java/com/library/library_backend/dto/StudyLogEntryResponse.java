package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyLogEntryResponse {
    private Long id;
    private String logDate;
    private String subject;
    private double hoursStudied;
    private int minutesStudied;
    private String notes;
    private String createdAt;
}
