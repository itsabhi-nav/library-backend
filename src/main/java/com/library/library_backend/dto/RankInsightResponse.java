package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankInsightResponse {
    private Integer currentRank;
    private Integer targetRank;
    private double yourHours;
    private double targetRankHours;
    private double hoursNeeded;
    private String message;
    private boolean atTop;
}
