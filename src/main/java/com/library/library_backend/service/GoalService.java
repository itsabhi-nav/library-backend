package com.library.library_backend.service;

import com.library.library_backend.dto.MonthlyGoalRequest;
import com.library.library_backend.dto.MonthlyGoalResponse;
import com.library.library_backend.model.User;
import com.library.library_backend.model.UserMonthlyGoal;
import com.library.library_backend.repository.DailyAttendanceSummaryRepository;
import com.library.library_backend.repository.UserMonthlyGoalRepository;
import com.library.library_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

@Service
public class GoalService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private UserMonthlyGoalRepository goalRepository;

    @Autowired
    private DailyAttendanceSummaryRepository dailySummaryRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public MonthlyGoalResponse getGoal(Long userId, Integer year, Integer month) {
        YearMonth ym = resolveYearMonth(year, month);
        UserMonthlyGoal goal = goalRepository.findByUserIdAndYearAndMonth(userId, ym.getYear(), ym.getMonthValue())
                .orElseGet(() -> createDefaultGoal(userId, ym));

        long completedMinutes = dailySummaryRepository.sumMinutesForUserInRange(
                userId, ym.atDay(1), ym.atEndOfMonth());

        return buildResponse(goal, completedMinutes);
    }

    @Transactional
    public MonthlyGoalResponse setGoal(Long userId, MonthlyGoalRequest request) {
        YearMonth ym = resolveYearMonth(request.getYear(), request.getMonth());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        int targetMinutes = request.getTargetHours() * 60;
        UserMonthlyGoal goal = goalRepository.findByUserIdAndYearAndMonth(userId, ym.getYear(), ym.getMonthValue())
                .orElse(UserMonthlyGoal.builder()
                        .user(user)
                        .year(ym.getYear())
                        .month(ym.getMonthValue())
                        .build());
        goal.setTargetMinutes(targetMinutes);
        goal = goalRepository.save(goal);

        long completedMinutes = dailySummaryRepository.sumMinutesForUserInRange(
                userId, ym.atDay(1), ym.atEndOfMonth());

        return buildResponse(goal, completedMinutes);
    }

    private UserMonthlyGoal createDefaultGoal(Long userId, YearMonth ym) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return goalRepository.save(UserMonthlyGoal.builder()
                .user(user)
                .year(ym.getYear())
                .month(ym.getMonthValue())
                .targetMinutes(UserMonthlyGoal.DEFAULT_TARGET_MINUTES)
                .build());
    }

    private MonthlyGoalResponse buildResponse(UserMonthlyGoal goal, long completedMinutes) {
        double progressPercent = goal.getTargetMinutes() > 0
                ? Math.min(100.0, Math.round((completedMinutes * 1000.0) / goal.getTargetMinutes()) / 10.0)
                : 0.0;

        return MonthlyGoalResponse.builder()
                .year(goal.getYear())
                .month(goal.getMonth())
                .targetMinutes(goal.getTargetMinutes())
                .targetHours(Math.round(goal.getTargetMinutes() / 6.0) / 10.0)
                .completedMinutes(completedMinutes)
                .completedHours(Math.round(completedMinutes / 6.0) / 10.0)
                .progressPercent(progressPercent)
                .build();
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        if (year != null && month != null) {
            return YearMonth.of(year, month);
        }
        return YearMonth.now(LIBRARY_ZONE);
    }
}
