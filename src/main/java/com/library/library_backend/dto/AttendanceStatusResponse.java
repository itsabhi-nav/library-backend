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
public class AttendanceStatusResponse {
    private boolean punchedIn;
    private String checkInTime;
    private Long assignedSeatId;
    private String seatNumber;
    private List<AchievementUnlockedResponse> newAchievements;
}
