package com.library.library_backend.service;

import com.library.library_backend.dto.*;
import com.library.library_backend.model.*;
import com.library.library_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class FeeService {

    @Autowired private FeeInvoiceRepository invoiceRepository;
    @Autowired private FeePaymentRepository paymentRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private LibraryConfigRepository configRepository;
    @Autowired private UserRepository userRepository;

    @Transactional
    public FeeGenerateResponse generateForMonth(int year, int month) {
        if (month < 1 || month > 12) throw new IllegalArgumentException("Invalid month");
        YearMonth ym = YearMonth.of(year, month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        int dueDay = getConfigInt("fee_due_day_of_month", 5);
        LocalDate dueDate = ym.atDay(Math.min(dueDay, ym.lengthOfMonth()));

        List<Subscription> subs = subscriptionRepository.findActiveSubscriptionsOverlappingMonth(monthStart, monthEnd);
        int created = 0, skipped = 0;

        for (Subscription sub : subs) {
            if (sub.getPlan() == null) { skipped++; continue; }
            if (invoiceRepository.findByUserIdAndBillingYearAndBillingMonth(sub.getUser().getId(), year, month).isPresent()) {
                skipped++;
                continue;
            }
            try {
                FeeInvoice invoice = FeeInvoice.builder()
                        .user(sub.getUser())
                        .subscription(sub)
                        .billingYear(year)
                        .billingMonth(month)
                        .amount(sub.getPlan().getPrice())
                        .planName(sub.getPlan().getName())
                        .dueDate(dueDate)
                        .status(FeeInvoice.InvoiceStatus.PENDING)
                        .amountPaid(BigDecimal.ZERO)
                        .build();
                invoiceRepository.save(invoice);
                created++;
            } catch (DataIntegrityViolationException ex) {
                // Race-safe: unique (user_id, billing_year, billing_month) prevents duplicates
                skipped++;
            }
        }
        return new FeeGenerateResponse(year, month, created, skipped);
    }

    @Transactional(readOnly = true)
    public Page<FeeInvoice> searchInvoices(Integer year, Integer month, String statusStr, String search, Pageable pageable) {
        FeeInvoice.InvoiceStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !"all".equalsIgnoreCase(statusStr)) {
            status = FeeInvoice.InvoiceStatus.valueOf(statusStr.toUpperCase());
        }
        Page<FeeInvoice> page = invoiceRepository.searchInvoices(year, month, status, search, pageable);
        page.getContent().forEach(this::refreshStatus);
        return page;
    }

    public FeeStatsResponse getStats(int year, int month) {
        refreshOverdueStatuses(year, month);
        BigDecimal outstanding = invoiceRepository.sumOutstandingForMonth(year, month);
        BigDecimal collected = invoiceRepository.sumCollectedForMonth(year, month);
        long outstandingCount = invoiceRepository.countOutstandingForMonth(year, month);
        long overdueCount = invoiceRepository.countOverdueForMonth(year, month);
        return new FeeStatsResponse(year, month, outstanding, collected, outstandingCount, overdueCount, outstandingCount + overdueCount);
    }

    @Transactional(readOnly = true)
    public List<FeeInvoice> getMyInvoices(Long userId) {
        List<FeeInvoice> invoices = invoiceRepository.findByUserIdOrderByBillingYearDescBillingMonthDesc(userId);
        invoices.forEach(this::refreshStatus);
        return invoices;
    }

    public FeeInvoice getCurrentMonthInvoice(Long userId) {
        LocalDate now = LocalDate.now();
        return invoiceRepository.findByUserIdAndBillingYearAndBillingMonth(userId, now.getYear(), now.getMonthValue())
                .map(inv -> { refreshStatus(inv); return inv; })
                .orElse(null);
    }

    @Transactional
    public FeeInvoice recordPayment(Long invoiceId, FeePaymentRequest request, Long recordedById) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        FeeInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        if (invoice.getStatus() == FeeInvoice.InvoiceStatus.WAIVED) {
            throw new IllegalArgumentException("Cannot record payment on waived invoice");
        }
        User recorder = userRepository.findById(recordedById)
                .orElseThrow(() -> new IllegalArgumentException("Recorder not found"));

        FeePayment payment = FeePayment.builder()
                .invoice(invoice)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH")
                .recordedBy(recorder)
                .notes(request.getNotes())
                .build();
        paymentRepository.save(payment);

        invoice.setAmountPaid(invoice.getAmountPaid().add(request.getAmount()));
        refreshStatus(invoice);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public FeeInvoice waiveInvoice(Long invoiceId) {
        FeeInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        invoice.setStatus(FeeInvoice.InvoiceStatus.WAIVED);
        return invoiceRepository.save(invoice);
    }

    private void refreshStatus(FeeInvoice invoice) {
        if (invoice.getStatus() == FeeInvoice.InvoiceStatus.WAIVED) return;
        BigDecimal remaining = invoice.getAmount().subtract(invoice.getAmountPaid());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(FeeInvoice.InvoiceStatus.PAID);
        } else if (invoice.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(FeeInvoice.InvoiceStatus.PARTIAL);
        } else if (LocalDate.now().isAfter(invoice.getDueDate())) {
            invoice.setStatus(FeeInvoice.InvoiceStatus.OVERDUE);
        } else {
            invoice.setStatus(FeeInvoice.InvoiceStatus.PENDING);
        }
    }

    @Transactional
    public void refreshOverdueStatuses(int year, int month) {
        List<FeeInvoice> candidates = invoiceRepository.findOverdueCandidates(year, month);
        LocalDate today = LocalDate.now();
        for (FeeInvoice inv : candidates) {
            if (today.isAfter(inv.getDueDate()) && inv.getAmountPaid().compareTo(inv.getAmount()) < 0) {
                if (inv.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
                    inv.setStatus(FeeInvoice.InvoiceStatus.PARTIAL);
                } else {
                    inv.setStatus(FeeInvoice.InvoiceStatus.OVERDUE);
                }
                invoiceRepository.save(inv);
            }
        }
    }

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void scheduledOverdueCheck() {
        LocalDate now = LocalDate.now();
        refreshOverdueStatuses(now.getYear(), now.getMonthValue());
    }

    private int getConfigInt(String key, int defaultVal) {
        return configRepository.findById(key)
                .map(c -> { try { return Integer.parseInt(c.getConfigValue()); } catch (NumberFormatException e) { return defaultVal; } })
                .orElse(defaultVal);
    }
}
