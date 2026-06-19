package com.library.library_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "membership_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String description;

    @NotNull
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @NotNull
    @Column(nullable = false)
    private BigDecimal price;

    /** Which shift/time-slot this plan covers. Null = covers all shifts. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shift_id")
    @JsonIgnoreProperties({"createdAt"})
    private Shift shift;

    /** Admin can deactivate plans; deactivated plans won't appear for new signups. */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
