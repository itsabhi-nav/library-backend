package com.library.library_backend.repository;

import com.library.library_backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByBookingDate(LocalDate bookingDate);

    List<Booking> findByUserId(Long userId);

    @Query("SELECT b FROM Booking b WHERE b.seat.id = :seatId AND b.shift.id = :shiftId AND b.bookingDate = :date AND b.status = 'ACTIVE'")
    Optional<Booking> findActiveBookingBySeatShiftAndDate(
            @Param("seatId") Long seatId,
            @Param("shiftId") Long shiftId,
            @Param("date") LocalDate date);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.bookingDate = :date AND b.status = 'ACTIVE'")
    List<Booking> findActiveBookingsForUserOnDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date);

    boolean existsBySeatId(Long seatId);
}
