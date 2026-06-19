package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private long totalMembers;
    private long activeMembers;
    private long checkedInNow;
    private long totalSeats;
    private long availableSeats;
    private BigDecimal feesOutstanding;
    private BigDecimal feesCollected;
    private long feesOverdueCount;
    private String libraryName;
}
