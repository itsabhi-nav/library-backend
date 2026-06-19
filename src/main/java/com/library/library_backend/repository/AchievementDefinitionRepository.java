package com.library.library_backend.repository;

import com.library.library_backend.model.AchievementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, Long> {

    List<AchievementDefinition> findAllByIsActiveTrueOrderBySortOrderAsc();

    Optional<AchievementDefinition> findByCode(String code);
}
