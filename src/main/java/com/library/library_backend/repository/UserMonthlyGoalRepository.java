package com.library.library_backend.repository;

import com.library.library_backend.model.UserMonthlyGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserMonthlyGoalRepository extends JpaRepository<UserMonthlyGoal, Long> {

    Optional<UserMonthlyGoal> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);
}
