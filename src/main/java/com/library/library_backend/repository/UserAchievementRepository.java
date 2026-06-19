package com.library.library_backend.repository;

import com.library.library_backend.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Set;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserIdOrderByEarnedAtDesc(Long userId);

    @Query("SELECT ua.achievementDefinition.id FROM UserAchievement ua WHERE ua.user.id = :userId")
    Set<Long> findEarnedDefinitionIdsByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndAchievementDefinitionId(Long userId, Long achievementDefinitionId);

    java.util.Optional<UserAchievement> findFirstByUserIdOrderByEarnedAtDesc(Long userId);
}
