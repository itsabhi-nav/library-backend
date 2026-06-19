package com.library.library_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeStatsResponse {
    private int year;
    private int month;
    private BigDecimal totalOutstanding;
    private BigDecimal totalCollected;
    private long outstandingCount;
    private long overdueCount;
    private long generatedCount;
}
