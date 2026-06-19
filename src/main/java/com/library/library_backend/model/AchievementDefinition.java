package com.library.library_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "achievement_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "threshold_value", nullable = false)
    private Integer thresholdValue;

    @Column(name = "threshold_unit", nullable = false, length = 32)
    private String thresholdUnit;

    @Column(name = "icon_key", nullable = false, length = 64)
    private String iconKey;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
