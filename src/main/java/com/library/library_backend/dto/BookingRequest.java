package com.library.library_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequest {
    @NotNull
    private Long seatId;

    @NotNull
    private Long shiftId;

    @NotNull
    private LocalDate bookingDate;
}
