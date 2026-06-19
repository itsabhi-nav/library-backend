package com.library.library_backend.repository;

import com.library.library_backend.model.LockerRental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LockerRentalRepository extends JpaRepository<LockerRental, Long> {

    List<LockerRental> findByUserId(Long userId);

    @Query("SELECT r FROM LockerRental r WHERE r.user.id = :userId AND r.status = 'ACTIVE' AND :today BETWEEN r.startDate AND r.endDate")
    Optional<LockerRental> findActiveRentalForUser(@Param("userId") Long userId, @Param("today") LocalDate today);
}
