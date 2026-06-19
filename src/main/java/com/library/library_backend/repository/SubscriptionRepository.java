package com.library.library_backend.repository;

import com.library.library_backend.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status = 'ACTIVE' AND :today BETWEEN s.startDate AND s.endDate")
    Optional<Subscription> findActiveSubscriptionForUser(@Param("userId") Long userId, @Param("today") LocalDate today);

    List<Subscription> findByUserId(Long userId);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan.id = :planId AND s.status = 'ACTIVE' AND :today BETWEEN s.startDate AND s.endDate")
    Long countActiveByPlanId(@Param("planId") Long planId, @Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(s.paidAmount), 0) FROM Subscription s WHERE s.plan.id = :planId")
    BigDecimal sumRevenueByPlanId(@Param("planId") Long planId);

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.user u
        JOIN FETCH s.plan p
        WHERE s.status = 'ACTIVE'
          AND s.startDate <= :monthEnd
          AND s.endDate >= :monthStart
        """)
    List<Subscription> findActiveSubscriptionsOverlappingMonth(
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);
}
