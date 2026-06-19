package com.library.library_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "seat_number", unique = true, nullable = false)
    private String seatNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Builder.Default
    @Column(name = "has_power_outlet")
    private Boolean hasPowerOutlet = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum SeatStatus {
        AVAILABLE,
        MAINTENANCE
    }
}
