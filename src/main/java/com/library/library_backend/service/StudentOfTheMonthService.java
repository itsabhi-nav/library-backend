package com.library.library_backend.service;

import com.library.library_backend.dto.StudentOfTheMonthResponse;
import com.library.library_backend.dto.StudentOfTheMonthWinnerResponse;
import com.library.library_backend.model.User;
import com.library.library_backend.repository.DailyAttendanceSummaryRepository;
import com.library.library_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentOfTheMonthService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyAttendanceSummaryRepository dailySummaryRepository;

    @Autowired
    private StreakCalculator streakCalculator;

    public StudentOfTheMonthResponse getStudentOfTheMonth(Integer year, Integer month) {
        YearMonth ym = resolveYearMonth(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<User> members = userRepository.findAllByRole(User.Role.MEMBER);
        Map<Long, long[]> monthStats = new HashMap<>();
        for (Object[] row : dailySummaryRepository.aggregateMinutesByUserInRange(start, end)) {
            Long userId = (Long) row[0];
            long minutes = ((Number) row[1]).longValue();
            long days = ((Number) row[2]).longValue();
            monthStats.put(userId, new long[]{minutes, days});
        }

        User topHours = null;
        long topHoursValue = -1;
        User topAttendance = null;
        long topAttendanceValue = -1;
        User topStreak = null;
        int topStreakValue = -1;

        for (User member : members) {
            long[] stats = monthStats.getOrDefault(member.getId(), new long[]{0L, 0L});
            if (stats[0] > topHoursValue) {
                topHoursValue = stats[0];
                topHours = member;
            }
            if (stats[1] > topAttendanceValue) {
                topAttendanceValue = stats[1];
                topAttendance = member;
            }

            List<LocalDate> monthDates = dailySummaryRepository.findPresentDatesByUserId(member.getId())
                    .stream()
                    .filter(d -> !d.isBefore(start) && !d.isAfter(end))
                    .toList();
            int streak = streakCalculator.calculate(monthDates).longestStreak();
            if (streak > topStreakValue) {
                topStreakValue = streak;
                topStreak = member;
            }
        }

        List<StudentOfTheMonthWinnerResponse> winners = new ArrayList<>();
        winners.add(buildWinner("HOURS", "Highest Study Hours", topHours, topHoursValue,
                formatHours(topHoursValue)));
        winners.add(buildWinner("ATTENDANCE", "Best Attendance", topAttendance, topAttendanceValue,
                topAttendanceValue + " day" + (topAttendanceValue != 1 ? "s" : "")));
        winners.add(buildWinner("STREAK", "Longest Streak", topStreak, topStreakValue,
                topStreakValue + " day" + (topStreakValue != 1 ? "s" : "")));

        return StudentOfTheMonthResponse.builder()
                .year(ym.getYear())
                .month(ym.getMonthValue())
                .winners(winners)
                .build();
    }

    private StudentOfTheMonthWinnerResponse buildWinner(
            String category, String label, User user, long value, String valueLabel) {
        if (user == null || value <= 0) {
            return StudentOfTheMonthWinnerResponse.builder()
                    .category(category)
                    .categoryLabel(label)
                    .value(0)
                    .valueLabel("—")
                    .build();
        }
        return StudentOfTheMonthWinnerResponse.builder()
                .category(category)
                .categoryLabel(label)
                .userId(user.getId())
                .memberId(user.getMemberId())
                .fullName(user.getFullName())
                .value(value)
                .valueLabel(valueLabel)
                .build();
    }

    private String formatHours(long minutes) {
        double hours = Math.round(minutes / 6.0) / 10.0;
        return hours + "h";
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        if (year != null && month != null) {
            return YearMonth.of(year, month);
        }
        return YearMonth.now(LIBRARY_ZONE);
    }
}
