package com.library.library_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SubscriptionRequest {
    @NotNull
    private Long userId;

    @NotNull
    private Long planId;

    @NotNull
    private BigDecimal paidAmount;

    @NotNull
    private String paymentMethod; // e.g. CASH, UPI, CARD
}
