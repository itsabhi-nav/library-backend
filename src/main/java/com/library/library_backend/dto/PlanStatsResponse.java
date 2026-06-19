package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

/** Per-plan subscriber count and revenue stats for the admin dashboard. */
@Data
@AllArgsConstructor
public class PlanStatsResponse {
    private Long       planId;
    private String     planName;
    private String     shiftName;
    private Long       activeSubscriberCount;
    private BigDecimal totalRevenue;
}
