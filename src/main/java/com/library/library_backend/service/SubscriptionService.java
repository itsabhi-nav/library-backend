package com.library.library_backend.service;

import com.library.library_backend.dto.PlanRequest;
import com.library.library_backend.dto.PlanStatsResponse;
import com.library.library_backend.dto.SubscriptionRequest;
import com.library.library_backend.model.MembershipPlan;
import com.library.library_backend.model.Shift;
import com.library.library_backend.model.Subscription;
import com.library.library_backend.model.User;
import com.library.library_backend.repository.MembershipPlanRepository;
import com.library.library_backend.repository.ShiftRepository;
import com.library.library_backend.repository.SubscriptionRepository;
import com.library.library_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    @Autowired private MembershipPlanRepository planRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ShiftRepository shiftRepository;

    // ── Plans ────────────────────────────────────────

    /** Returns all active plans (visible to students and admin). */
    public List<MembershipPlan> getAllPlans() {
        return planRepository.findAllByIsActiveTrue();
    }

    /** Returns ALL plans including deactivated ones (admin only). */
    public List<MembershipPlan> getAllPlansAdmin() {
        return planRepository.findAll();
    }

    @Transactional
    public MembershipPlan createPlan(PlanRequest request) {
        MembershipPlan plan = new MembershipPlan();
        applyPlanRequest(plan, request);
        return planRepository.save(plan);
    }

    @Transactional
    public MembershipPlan updatePlan(Long id, PlanRequest request) {
        MembershipPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));
        applyPlanRequest(plan, request);
        return planRepository.save(plan);
    }

    @Transactional
    public void deactivatePlan(Long id) {
        MembershipPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));
        plan.setIsActive(false);
        planRepository.save(plan);
    }

    private void applyPlanRequest(MembershipPlan plan, PlanRequest request) {
        if (request.getName() != null && !request.getName().isBlank())
            plan.setName(request.getName());
        if (request.getDescription() != null)
            plan.setDescription(request.getDescription());
        if (request.getDurationDays() != null)
            plan.setDurationDays(request.getDurationDays());
        if (request.getPrice() != null)
            plan.setPrice(BigDecimal.valueOf(request.getPrice()));
        if (request.getIsActive() != null)
            plan.setIsActive(request.getIsActive());
        // Link to shift
        if (request.getShiftId() != null) {
            Shift shift = shiftRepository.findById(request.getShiftId())
                    .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + request.getShiftId()));
            plan.setShift(shift);
        } else if (request.getShiftId() == null && request.getName() != null) {
            // Explicitly clearing the shift if a full update is done
            plan.setShift(null);
        }
    }

    /** Per-plan stats: active subscriber count + total revenue. */
    public List<PlanStatsResponse> getPlanStats() {
        List<MembershipPlan> allPlans = planRepository.findAll();
        LocalDate today = LocalDate.now();
        return allPlans.stream().map(plan -> {
            Long count = subscriptionRepository.countActiveByPlanId(plan.getId(), today);
            BigDecimal revenue = subscriptionRepository.sumRevenueByPlanId(plan.getId());
            String shiftName = plan.getShift() != null ? plan.getShift().getName() : "All Shifts";
            return new PlanStatsResponse(plan.getId(), plan.getName(), shiftName, count, revenue);
        }).collect(Collectors.toList());
    }

    // ── Subscriptions ────────────────────────────────

    @Transactional
    public Subscription createSubscription(SubscriptionRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        MembershipPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Membership plan not found"));

        // Expire any existing active subscription
        LocalDate today = LocalDate.now();
        subscriptionRepository.findActiveSubscriptionForUser(user.getId(), today)
                .ifPresent(old -> {
                    old.setStatus(Subscription.SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(old);
                });

        LocalDate startDate = LocalDate.now();
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .startDate(startDate)
                .endDate(startDate.plusDays(plan.getDurationDays()))
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .paidAmount(request.getPaidAmount())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(Subscription.PaymentStatus.PAID)
                .build();
        return subscriptionRepository.save(subscription);
    }

    public Optional<Subscription> getActiveSubscription(Long userId) {
        return subscriptionRepository.findActiveSubscriptionForUser(userId, LocalDate.now());
    }

    public List<Subscription> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }
}
