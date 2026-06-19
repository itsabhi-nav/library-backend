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
public class UserAchievementsResponse {
    private int earnedCount;
    private int totalCount;
    private int currentStreak;
    private int longestStreak;
    private List<AchievementProgressResponse> achievements;
}
