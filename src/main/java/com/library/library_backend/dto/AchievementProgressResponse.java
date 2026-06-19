package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementProgressResponse {
    private Long id;
    private String code;
    private String category;
    private String title;
    private String description;
    private String iconKey;
    private int thresholdValue;
    private String thresholdUnit;
    private int sortOrder;
    private boolean earned;
    private String earnedAt;
    private int progressPercent;
    private int currentValue;
}
