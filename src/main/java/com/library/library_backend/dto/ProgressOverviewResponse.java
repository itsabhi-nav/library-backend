package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressOverviewResponse {
    private int year;
    private int month;
    private UserExamTargetResponse examCountdown;
    private CompareAverageResponse compareAverage;
    private RankInsightResponse rankInsight;
    private AchievementUnlockedResponse latestMilestone;
    private int earnedBadgeCount;
    private int totalBadgeCount;
}
