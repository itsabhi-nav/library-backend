package com.library.library_backend.service;

import com.library.library_backend.config.PasswordUtil;
import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.*;
import com.library.library_backend.model.*;
import com.library.library_backend.repository.*;
import com.library.library_backend.whatsapp.service.LibraryAdmissionNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private TokenService tokenService;
    @Autowired private LibraryConfigRepository configRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private MembershipPlanRepository planRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private LibraryAdmissionNotificationService admissionNotificationService;

    @Transactional
    public String generateNextMemberId() {
        String prefix = configRepository.findById("member_id_prefix")
                .map(LibraryConfig::getConfigValue)
                .orElse("LIB")
                .toUpperCase()
                .replaceAll("[^A-Z0-9]", "");

        int digits = configRepository.findById("member_id_digits")
                .map(c -> { try { return Integer.parseInt(c.getConfigValue()); } catch (NumberFormatException e) { return 3; } })
                .orElse(3);

        LibraryConfig counterConfig = configRepository.findById("member_id_counter")
                .orElseThrow(() -> new IllegalStateException("member_id_counter config missing"));

        int next = Integer.parseInt(counterConfig.getConfigValue()) + 1;
        counterConfig.setConfigValue(String.valueOf(next));
        configRepository.save(counterConfig);

        return prefix + String.format("%0" + digits + "d", next);
    }

    private void validateSeatAssignment(Long seatId, Long excludeUserId) {
        if (seatId == null) return;
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));
        if (seat.getStatus() == Seat.SeatStatus.MAINTENANCE) {
            throw new IllegalArgumentException("Seat is under maintenance");
        }
        if (userRepository.isSeatTakenByAnotherActiveMember(seatId, excludeUserId)) {
            throw new IllegalArgumentException("Seat " + seat.getSeatNumber() + " is already assigned to another active student");
        }
    }

    @Transactional
    public StudentRegisterResponse registerStudent(StudentRegisterRequest request) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String memberId = generateNextMemberId();
        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword()
                : request.getPhoneNumber();

        Seat assignedSeat = null;
        if (request.getSeatId() != null) {
            validateSeatAssignment(request.getSeatId(), null);
            assignedSeat = seatRepository.findById(request.getSeatId())
                    .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + request.getSeatId()));
        }

        User user = User.builder()
                .memberId(memberId)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .dob(request.getDob())
                .whatsappConsent(request.getWhatsappConsent() != null ? request.getWhatsappConsent() : true)
                .passwordHash(PasswordUtil.hashPassword(rawPassword))
                .role(User.Role.MEMBER)
                .isActive(true)
                .assignedSeat(assignedSeat)
                .build();

        user = userRepository.save(user);

        if (request.getPlanId() != null) {
            MembershipPlan plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + request.getPlanId()));
            LocalDate start = LocalDate.now();
            Subscription sub = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .startDate(start)
                    .endDate(start.plusDays(plan.getDurationDays()))
                    .status(Subscription.SubscriptionStatus.ACTIVE)
                    .paidAmount(plan.getPrice())
                    .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH")
                    .paymentStatus(Subscription.PaymentStatus.PAID)
                    .build();
            subscriptionRepository.save(sub);
        }

        StudentRegisterResponse response = new StudentRegisterResponse(
                user.getId(), user.getMemberId(), user.getFullName(), user.getPhoneNumber(),
                user.getAddress(), user.getRole().name(),
                assignedSeat != null ? assignedSeat.getSeatNumber() : null,
                rawPassword
        );

        admissionNotificationService.sendAdmissionConfirmationIfNeeded(
                user.getFullName(), user.getPhoneNumber(), user.getId(), user.getWhatsappConsent());

        return response;
    }

    public User registerMember(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(PasswordUtil.hashPassword(request.getPassword()))
                .role(User.Role.MEMBER)
                .phoneNumber(request.getPhoneNumber())
                .isActive(true)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public LoginResponse authenticate(LoginRequest request) {
        String memberId = request.getMemberId() != null ? request.getMemberId().trim() : "";
        User user = findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Member ID or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Account is deactivated. Please contact the admin.");
        }

        if (!PasswordUtil.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid Member ID or password");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String token = tokenService.generateToken(user.getId(), user.getMemberId(), user.getRole().name());
        return new LoginResponse(token, user.getId(), user.getMemberId(), user.getFullName(), user.getRole().name(), user.getPhoneNumber());
    }

    @Transactional
    public User setActiveStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setIsActive(active);
        return userRepository.save(user);
    }

    @Transactional
    public User resetPassword(Long userId, String newPassword) {
        validatePassword(newPassword);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPasswordHash(PasswordUtil.hashPassword(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public void changeOwnPassword(Long userId, String currentPassword, String newPassword) {
        validatePassword(newPassword);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!PasswordUtil.verifyPassword(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (PasswordUtil.verifyPassword(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }
        user.setPasswordHash(PasswordUtil.hashPassword(newPassword));
        userRepository.save(user);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }
    }

    @Transactional
    public User updateStudent(Long userId, StudentRegisterRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (request.getFullName() != null && !request.getFullName().isBlank())
            user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank())
            user.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null)
            user.setAddress(request.getAddress());
        if (request.getDob() != null)
            user.setDob(request.getDob());
        if (request.getWhatsappConsent() != null)
            user.setWhatsappConsent(request.getWhatsappConsent());
        if (Boolean.TRUE.equals(request.getAssignSeat())) {
            if (request.getSeatId() != null) {
                validateSeatAssignment(request.getSeatId(), userId);
                Seat seat = seatRepository.findById(request.getSeatId())
                        .orElseThrow(() -> new IllegalArgumentException("Seat not found"));
                user.setAssignedSeat(seat);
            } else {
                user.setAssignedSeat(null);
            }
        } else if (request.getSeatId() != null) {
            validateSeatAssignment(request.getSeatId(), userId);
            Seat seat = seatRepository.findById(request.getSeatId())
                    .orElseThrow(() -> new IllegalArgumentException("Seat not found"));
            user.setAssignedSeat(seat);
        }
        user = userRepository.save(user);

        maybeSendAdmissionWhatsApp(user);

        return user;
    }

    /** Welcome message when consent is on and not already sent for this member. */
    private void maybeSendAdmissionWhatsApp(User user) {
        admissionNotificationService.sendAdmissionConfirmationIfNeeded(
                user.getFullName(), user.getPhoneNumber(), user.getId(), user.getWhatsappConsent());
    }

    public Optional<User> findById(Long id) { return userRepository.findById(id); }

    public Optional<User> findByMemberId(String memberId) {
        if (memberId == null || memberId.isBlank()) return Optional.empty();
        String normalized = normalizeMemberId(memberId);
        return userRepository.findByMemberIdNormalized(normalized)
                .or(() -> userRepository.findByMemberId(memberId.trim()));
    }

    private static String normalizeMemberId(String memberId) {
        return memberId.trim().toUpperCase().replace("-", "").replace(" ", "");
    }

    public Optional<User> findByEmail(String email) { return userRepository.findByEmail(email); }
    public List<User> getAllMembers() { return userRepository.findAllByRole(User.Role.MEMBER); }
    public List<User> getAllUsers() { return userRepository.findAllByOrderByCreatedAtDesc(); }

    public Page<User> searchStudents(String search, String status, Pageable pageable) {
        String st = (status == null || status.isBlank()) ? "all" : status.toLowerCase();
        return userRepository.searchStudents(search, st, pageable);
    }

    public long countActiveMembers() { return userRepository.countActiveMembers(); }
    public long countAllMembers() { return userRepository.countAllMembers(); }
}
