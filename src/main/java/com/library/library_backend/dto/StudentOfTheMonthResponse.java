package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentOfTheMonthResponse {
    private int year;
    private int month;
    private List<StudentOfTheMonthWinnerResponse> winners;
}
