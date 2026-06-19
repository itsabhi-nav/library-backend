package com.library.library_backend.service;

import com.library.library_backend.dto.AchievementUnlockedResponse;
import com.library.library_backend.dto.AttendanceStatusResponse;
import com.library.library_backend.model.Attendance;
import com.library.library_backend.model.Booking;
import com.library.library_backend.model.Seat;
import com.library.library_backend.model.User;
import com.library.library_backend.repository.AttendanceRepository;
import com.library.library_backend.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AttendanceStatsService attendanceStatsService;

    @Autowired
    private AchievementEvaluationService achievementEvaluationService;

    public Attendance checkIn(String memberId) {
        User user = userService.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("User with member ID " + memberId + " not found"));

        Optional<Attendance> activeAttendance = attendanceRepository.findActiveAttendanceByUserId(user.getId());
        if (activeAttendance.isPresent()) {
            return activeAttendance.get();
        }

        List<Booking> bookingsToday = bookingRepository.findActiveBookingsForUserOnDate(user.getId(), LocalDate.now());
        Booking booking = bookingsToday.isEmpty() ? null : bookingsToday.get(0);

        Attendance attendance = Attendance.builder()
                .user(user)
                .booking(booking)
                .checkInTime(OffsetDateTime.now())
                .build();

        return attendanceRepository.save(attendance);
    }

    @Transactional
    public Attendance checkOut(String memberId) {
        User user = userService.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("User with member ID " + memberId + " not found"));

        Attendance attendance = attendanceRepository.findActiveAttendanceByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User is not currently checked in"));

        attendance.setCheckOutTime(OffsetDateTime.now());
        Attendance saved = attendanceRepository.save(attendance);
        attendanceStatsService.recordSessionCompletion(saved);
        achievementEvaluationService.evaluateAndAward(user.getId());
        return saved;
    }

    @Transactional
    public AttendanceStatusResponse punchInSelf(Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getAssignedSeat() == null) {
            throw new IllegalArgumentException("No assigned seat. Contact admin to assign a seat before punching in.");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Account is deactivated.");
        }

        Optional<Attendance> active = attendanceRepository.findActiveAttendanceByUserId(userId);
        if (active.isPresent()) {
            return toStatus(active.get(), user);
        }

        List<Booking> bookingsToday = bookingRepository.findActiveBookingsForUserOnDate(userId, LocalDate.now());
        Booking booking = bookingsToday.isEmpty() ? null : bookingsToday.get(0);

        Attendance saved = attendanceRepository.save(Attendance.builder()
                .user(user)
                .booking(booking)
                .checkInTime(OffsetDateTime.now())
                .build());
        return toStatus(saved, user);
    }

    @Transactional
    public AttendanceStatusResponse punchOutSelf(Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Attendance attendance = attendanceRepository.findActiveAttendanceByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("You are not punched in"));
        attendance.setCheckOutTime(OffsetDateTime.now());
        Attendance saved = attendanceRepository.save(attendance);
        attendanceStatsService.recordSessionCompletion(saved);
        List<AchievementUnlockedResponse> newAchievements = achievementEvaluationService.evaluateAndAward(userId);
        return AttendanceStatusResponse.builder()
                .punchedIn(false)
                .assignedSeatId(user.getAssignedSeat() != null ? user.getAssignedSeat().getId() : null)
                .seatNumber(user.getAssignedSeat() != null ? user.getAssignedSeat().getSeatNumber() : null)
                .newAchievements(newAchievements.isEmpty() ? null : newAchievements)
                .build();
    }

    public AttendanceStatusResponse getMyStatus(Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return attendanceRepository.findActiveAttendanceByUserId(userId)
                .map(a -> toStatus(a, user))
                .orElseGet(() -> AttendanceStatusResponse.builder()
                        .punchedIn(false)
                        .assignedSeatId(user.getAssignedSeat() != null ? user.getAssignedSeat().getId() : null)
                        .seatNumber(user.getAssignedSeat() != null ? user.getAssignedSeat().getSeatNumber() : null)
                        .build());
    }

    public List<Attendance> getActiveSessions() {
        return attendanceRepository.findAllActiveAttendances();
    }

    public List<Long> getSeatIdsOccupiedByPunchIn() {
        return attendanceRepository.findSeatIdsWithActivePunchIn();
    }

    private AttendanceStatusResponse toStatus(Attendance attendance, User user) {
        Seat seat = user.getAssignedSeat();
        return AttendanceStatusResponse.builder()
                .punchedIn(true)
                .checkInTime(attendance.getCheckInTime() != null ? attendance.getCheckInTime().toString() : null)
                .assignedSeatId(seat != null ? seat.getId() : null)
                .seatNumber(seat != null ? seat.getSeatNumber() : null)
                .build();
    }
}
