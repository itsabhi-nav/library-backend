package com.library.library_backend.repository;

import com.library.library_backend.model.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {
    List<FeePayment> findByInvoiceIdOrderByPaidAtDesc(Long invoiceId);
}
