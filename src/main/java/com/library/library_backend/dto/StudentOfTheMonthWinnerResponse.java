package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentOfTheMonthWinnerResponse {
    private String category;
    private String categoryLabel;
    private Long userId;
    private String memberId;
    private String fullName;
    private long value;
    private String valueLabel;
}
