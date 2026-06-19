package com.library.library_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lockers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Locker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "locker_number", unique = true, nullable = false)
    private String lockerNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LockerStatus status;

    @NotNull
    @Column(name = "price_per_month", nullable = false)
    private BigDecimal pricePerMonth;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum LockerStatus {
        AVAILABLE,
        OCCUPIED,
        MAINTENANCE
    }
}
