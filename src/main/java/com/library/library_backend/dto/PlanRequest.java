package com.library.library_backend.dto;

import lombok.Data;

/** Used by admin to update or create a membership plan. */
@Data
public class PlanRequest {
    private String  name;
    private String  description;
    private Integer durationDays;
    private Double  price;
    private Long    shiftId;   // nullable — plan covers all shifts if omitted
    private Boolean isActive;
}
