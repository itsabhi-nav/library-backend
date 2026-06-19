package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDefinitionResponse {
    private String code;
    private String name;
    private String examLabel;
    private String examDate;
    private long daysRemaining;
}
