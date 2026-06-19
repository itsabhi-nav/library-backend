package com.library.library_backend.service;

import com.library.library_backend.dto.PersonalAnalyticsResponse;
import com.library.library_backend.repository.DailyAttendanceSummaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
public class UserMetricsService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private DailyAttendanceSummaryRepository dailySummaryRepository;

    @Autowired
    private StreakCalculator streakCalculator;

    public record UserMetrics(
            long lifetimeMinutes,
            int lifetimeDaysPresent,
            int currentStreak,
            int longestStreak,
            long monthMinutes,
            int monthDaysPresent) {}

    public UserMetrics getMetrics(Long userId, YearMonth ym) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<LocalDate> presentDates = dailySummaryRepository.findPresentDatesByUserId(userId);
        StreakCalculator.StreakResult streak = streakCalculator.calculate(presentDates);

        return new UserMetrics(
                dailySummaryRepository.sumLifetimeMinutes(userId),
                dailySummaryRepository.countLifetimePresentDays(userId),
                streak.currentStreak(),
                streak.longestStreak(),
                dailySummaryRepository.sumMinutesForUserInRange(userId, start, end),
                dailySummaryRepository.countPresentDaysForUserInRange(userId, start, end));
    }

    public PersonalAnalyticsResponse getPersonalAnalytics(Long userId, Integer year, Integer month) {
        YearMonth ym = resolveYearMonth(year, month);
        UserMetrics metrics = getMetrics(userId, ym);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        LocalDate today = LocalDate.now(LIBRARY_ZONE);

        int daysElapsed = ym.equals(YearMonth.from(today.atStartOfDay(LIBRARY_ZONE)))
                ? today.getDayOfMonth()
                : ym.lengthOfMonth();

        double avgDailyHours = metrics.monthDaysPresent() > 0
                ? roundHours(metrics.monthMinutes()) / metrics.monthDaysPresent()
                : 0.0;

        double attendancePercent = daysElapsed > 0
                ? Math.round((metrics.monthDaysPresent() * 1000.0) / daysElapsed) / 10.0
                : 0.0;

        String bestStudyDay = null;
        long bestStudyDayMinutes = 0;
        List<Object[]> bestDay = dailySummaryRepository.findBestStudyDayInRange(userId, start, end);
        if (!bestDay.isEmpty() && bestDay.get(0)[0] != null) {
            bestStudyDay = bestDay.get(0)[0].toString();
            bestStudyDayMinutes = ((Number) bestDay.get(0)[1]).longValue();
        }

        return PersonalAnalyticsResponse.builder()
                .year(ym.getYear())
                .month(ym.getMonthValue())
                .totalMinutesThisMonth(metrics.monthMinutes())
                .totalHoursThisMonth(roundHours(metrics.monthMinutes()))
                .averageDailyHours(Math.round(avgDailyHours * 10.0) / 10.0)
                .bestStudyDay(bestStudyDay)
                .bestStudyDayMinutes(bestStudyDayMinutes)
                .currentStreak(metrics.currentStreak())
                .longestStreak(metrics.longestStreak())
                .attendancePercent(attendancePercent)
                .daysPresentThisMonth(metrics.monthDaysPresent())
                .daysElapsedInMonth(daysElapsed)
                .build();
    }

    private double roundHours(long totalMinutes) {
        return Math.round(totalMinutes / 6.0) / 10.0;
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        if (year != null && month != null) {
            return YearMonth.of(year, month);
        }
        return YearMonth.now(LIBRARY_ZONE);
    }
}
