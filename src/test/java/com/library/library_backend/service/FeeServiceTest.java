package com.library.library_backend.service;

import com.library.library_backend.model.FeeInvoice;
import com.library.library_backend.model.MembershipPlan;
import com.library.library_backend.model.Subscription;
import com.library.library_backend.model.User;
import com.library.library_backend.repository.FeeInvoiceRepository;
import com.library.library_backend.repository.FeePaymentRepository;
import com.library.library_backend.repository.LibraryConfigRepository;
import com.library.library_backend.repository.SubscriptionRepository;
import com.library.library_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeServiceTest {

    @Mock private FeePaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private FeeInvoiceRepository invoiceRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private LibraryConfigRepository configRepository;

    @InjectMocks private FeeService feeService;

    @Test
    void generateForMonth_skipsExistingInvoiceAndCreatesOnlyForNewMember() {
        User existing = User.builder().id(1L).memberId("BR001").build();
        User newcomer = User.builder().id(2L).memberId("BR002").build();
        MembershipPlan plan = MembershipPlan.builder().id(1L).name("Basic").price(BigDecimal.valueOf(1000)).build();

        Subscription sub1 = Subscription.builder().user(existing).plan(plan)
                .startDate(LocalDate.of(2026, 6, 1)).endDate(LocalDate.of(2026, 6, 30)).build();
        Subscription sub2 = Subscription.builder().user(newcomer).plan(plan)
                .startDate(LocalDate.of(2026, 6, 15)).endDate(LocalDate.of(2026, 7, 15)).build();

        when(configRepository.findById("fee_due_day_of_month")).thenReturn(Optional.empty());
        when(subscriptionRepository.findActiveSubscriptionsOverlappingMonth(any(), any()))
                .thenReturn(List.of(sub1, sub2));
        when(invoiceRepository.findByUserIdAndBillingYearAndBillingMonth(1L, 2026, 6))
                .thenReturn(Optional.of(FeeInvoice.builder().id(99L).build()));
        when(invoiceRepository.findByUserIdAndBillingYearAndBillingMonth(2L, 2026, 6))
                .thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = feeService.generateForMonth(2026, 6);

        assertEquals(1, result.getCreated());
        assertEquals(1, result.getSkipped());
        verify(invoiceRepository, times(1)).save(any(FeeInvoice.class));
    }
}
