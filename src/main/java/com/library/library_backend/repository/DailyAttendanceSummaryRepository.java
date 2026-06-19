package com.library.library_backend.repository;

import com.library.library_backend.model.DailyAttendanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAttendanceSummaryRepository extends JpaRepository<DailyAttendanceSummary, Long> {

    Optional<DailyAttendanceSummary> findByUserIdAndAttendanceDate(Long userId, LocalDate attendanceDate);

    @Query("""
            SELECT COALESCE(SUM(d.totalMinutes), 0) FROM DailyAttendanceSummary d
            WHERE d.user.id = :userId
              AND d.attendanceDate >= :startDate AND d.attendanceDate <= :endDate
            """)
    long sumMinutesForUserInRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COUNT(d) FROM DailyAttendanceSummary d
            WHERE d.user.id = :userId
              AND d.attendanceDate >= :startDate AND d.attendanceDate <= :endDate
              AND d.totalMinutes > 0
            """)
    int countPresentDaysForUserInRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT d.user.id, COALESCE(SUM(d.totalMinutes), 0), COUNT(CASE WHEN d.totalMinutes > 0 THEN 1 END)
            FROM DailyAttendanceSummary d
            WHERE d.attendanceDate >= :startDate AND d.attendanceDate <= :endDate
            GROUP BY d.user.id
            """)
    List<Object[]> aggregateMinutesByUserInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(d.totalMinutes), 0) FROM DailyAttendanceSummary d
            WHERE d.user.id = :userId
            """)
    long sumLifetimeMinutes(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(d) FROM DailyAttendanceSummary d
            WHERE d.user.id = :userId AND d.totalMinutes > 0
            """)
    int countLifetimePresentDays(@Param("userId") Long userId);

    @Query("""
            SELECT d.attendanceDate FROM DailyAttendanceSummary d
            WHERE d.user.id = :userId AND d.totalMinutes > 0
            ORDER BY d.attendanceDate ASC
            """)
    List<LocalDate> findPresentDatesByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT d.attendanceDate, d.totalMinutes FROM DailyAttendanceSummary d
            WHERE d.user.id = :userId
              AND d.attendanceDate >= :startDate AND d.attendanceDate <= :endDate
            ORDER BY d.totalMinutes DESC
            LIMIT 1
            """)
    List<Object[]> findBestStudyDayInRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
