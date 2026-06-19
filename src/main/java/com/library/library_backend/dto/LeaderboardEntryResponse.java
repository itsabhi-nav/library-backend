package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryResponse {
    private int rank;
    private Long userId;
    private String memberId;
    private String fullName;
    private int daysPresent;
    private long totalMinutes;
    private double totalHours;
    /** GOLD, SILVER, BRONZE, or null for ranks 4+ */
    private String badge;
    private boolean currentUser;
}
