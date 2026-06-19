package com.library.library_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_monthly_goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMonthlyGoal {

    public static final int DEFAULT_TARGET_MINUTES = 9000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(name = "target_minutes", nullable = false)
    @Builder.Default
    private Integer targetMinutes = DEFAULT_TARGET_MINUTES;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
