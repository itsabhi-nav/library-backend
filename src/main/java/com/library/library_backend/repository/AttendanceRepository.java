package com.library.library_backend.repository;

import com.library.library_backend.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByUserId(Long userId);

    @Query("SELECT a FROM Attendance a WHERE a.user.id = :userId AND a.checkOutTime IS NULL")
    Optional<Attendance> findActiveAttendanceByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Attendance a WHERE a.checkOutTime IS NULL")
    List<Attendance> findAllActiveAttendances();

    long countByCheckOutTimeIsNull();

    @Query("""
            SELECT DISTINCT u.assignedSeat.id FROM Attendance a
            JOIN a.user u
            WHERE a.checkOutTime IS NULL AND u.assignedSeat IS NOT NULL
            """)
    List<Long> findSeatIdsWithActivePunchIn();

    @Query(value = """
            SELECT EXISTS(
                SELECT 1 FROM attendance a
                WHERE a.user_id = :userId
                  AND EXTRACT(HOUR FROM a.check_in_time AT TIME ZONE 'Asia/Kolkata') * 60
                      + EXTRACT(MINUTE FROM a.check_in_time AT TIME ZONE 'Asia/Kolkata') < :minutesOfDay
            )
            """, nativeQuery = true)
    boolean hasCheckInBeforeMinutesOfDay(@Param("userId") Long userId, @Param("minutesOfDay") int minutesOfDay);

    @Query(value = """
            SELECT EXISTS(
                SELECT 1 FROM attendance a
                WHERE a.user_id = :userId
                  AND EXTRACT(HOUR FROM a.check_in_time AT TIME ZONE 'Asia/Kolkata') * 60
                      + EXTRACT(MINUTE FROM a.check_in_time AT TIME ZONE 'Asia/Kolkata') >= :minutesOfDay
            )
            """, nativeQuery = true)
    boolean hasCheckInAtOrAfterMinutesOfDay(@Param("userId") Long userId, @Param("minutesOfDay") int minutesOfDay);
}
