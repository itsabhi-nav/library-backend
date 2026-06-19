package com.library.library_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fee_invoices")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "subscription", "payments"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "billing_year", nullable = false)
    private Integer billingYear;

    @Column(name = "billing_month", nullable = false)
    private Integer billingMonth;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(name = "amount_paid", nullable = false)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "generated_at", insertable = false, updatable = false)
    private OffsetDateTime generatedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FeePayment> payments = new ArrayList<>();

    public enum InvoiceStatus {
        PENDING, PAID, OVERDUE, PARTIAL, WAIVED
    }
}
