package com.library.library_backend.repository;

import com.library.library_backend.model.DailyStudyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyStudyLogRepository extends JpaRepository<DailyStudyLog, Long> {

    @Query("""
            SELECT l FROM DailyStudyLog l
            WHERE l.user.id = :userId
              AND l.logDate >= :startDate AND l.logDate <= :endDate
            ORDER BY l.logDate DESC, l.createdAt DESC
            """)
    List<DailyStudyLog> findByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(l.minutesStudied), 0) FROM DailyStudyLog l
            WHERE l.user.id = :userId
              AND l.logDate >= :startDate AND l.logDate <= :endDate
            """)
    long sumMinutesForUserInRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
