package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompareAverageResponse {
    private double yourHours;
    private double libraryAverageHours;
    private double differenceHours;
    private String message;
}
