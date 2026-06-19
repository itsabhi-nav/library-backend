package com.library.library_backend.repository;

import com.library.library_backend.model.FeeInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeeInvoiceRepository extends JpaRepository<FeeInvoice, Long> {

    Optional<FeeInvoice> findByUserIdAndBillingYearAndBillingMonth(Long userId, int year, int month);

    List<FeeInvoice> findByUserIdOrderByBillingYearDescBillingMonthDesc(Long userId);

    @Query("""
        SELECT fi FROM FeeInvoice fi
        JOIN fi.user u
        WHERE (:year IS NULL OR fi.billingYear = :year)
          AND (:month IS NULL OR fi.billingMonth = :month)
          AND (:status IS NULL OR fi.status = :status)
          AND (:search IS NULL OR :search = '' OR
               LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.memberId) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY fi.billingYear DESC, fi.billingMonth DESC, u.fullName ASC
        """)
    Page<FeeInvoice> searchInvoices(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("status") FeeInvoice.InvoiceStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
        SELECT fi FROM FeeInvoice fi
        WHERE fi.billingYear = :year AND fi.billingMonth = :month
          AND fi.status IN ('PENDING', 'OVERDUE', 'PARTIAL')
        ORDER BY fi.dueDate ASC
        """)
    List<FeeInvoice> findOverdueCandidates(@Param("year") int year, @Param("month") int month);

    @Query("""
        SELECT COALESCE(SUM(fi.amount - fi.amountPaid), 0) FROM FeeInvoice fi
        WHERE fi.billingYear = :year AND fi.billingMonth = :month
          AND fi.status IN ('PENDING', 'OVERDUE', 'PARTIAL')
        """)
    BigDecimal sumOutstandingForMonth(@Param("year") int year, @Param("month") int month);

    @Query("""
        SELECT COALESCE(SUM(fi.amountPaid), 0) FROM FeeInvoice fi
        WHERE fi.billingYear = :year AND fi.billingMonth = :month
        """)
    BigDecimal sumCollectedForMonth(@Param("year") int year, @Param("month") int month);

    @Query("""
        SELECT COUNT(fi) FROM FeeInvoice fi
        WHERE fi.billingYear = :year AND fi.billingMonth = :month
          AND fi.status IN ('PENDING', 'OVERDUE', 'PARTIAL')
        """)
    long countOutstandingForMonth(@Param("year") int year, @Param("month") int month);

    @Query("""
        SELECT COUNT(fi) FROM FeeInvoice fi
        WHERE fi.billingYear = :year AND fi.billingMonth = :month
          AND fi.status = 'OVERDUE'
        """)
    long countOverdueForMonth(@Param("year") int year, @Param("month") int month);
}
