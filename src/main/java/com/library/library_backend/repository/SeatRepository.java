package com.library.library_backend.repository;

import com.library.library_backend.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByStatus(Seat.SeatStatus status);
    Optional<Seat> findBySeatNumber(String seatNumber);
}
