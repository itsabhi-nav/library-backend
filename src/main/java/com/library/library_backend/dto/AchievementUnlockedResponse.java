package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementUnlockedResponse {
    private String code;
    private String title;
    private String description;
    private String category;
    private String iconKey;
    private String earnedAt;
}
