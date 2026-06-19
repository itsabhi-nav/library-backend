package com.library.library_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_achievements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "achievement_definition_id", nullable = false)
    private AchievementDefinition achievementDefinition;

    @Column(name = "earned_at", nullable = false)
    @Builder.Default
    private OffsetDateTime earnedAt = OffsetDateTime.now();
}
