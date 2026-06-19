package com.library.library_backend.dto;

import lombok.Data;

/** Used by admin to create or update a shift. */
@Data
public class ShiftRequest {
    private String name;
    private String startTime; // "HH:mm" e.g. "08:00"
    private String endTime;   // "HH:mm" e.g. "14:00"
}
