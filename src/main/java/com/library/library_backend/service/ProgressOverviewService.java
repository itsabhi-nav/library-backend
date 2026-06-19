package com.library.library_backend.service;

import com.library.library_backend.dto.*;
import com.library.library_backend.model.UserAchievement;
import com.library.library_backend.repository.DailyAttendanceSummaryRepository;
import com.library.library_backend.repository.UserAchievementRepository;
import com.library.library_backend.repository.UserRepository;
import com.library.library_backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
public class ProgressOverviewService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private AttendanceStatsService attendanceStatsService;

    @Autowired
    private ExamTargetService examTargetService;

    @Autowired
    private AchievementEvaluationService achievementEvaluationService;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private DailyAttendanceSummaryRepository dailySummaryRepository;

    @Autowired
    private UserRepository userRepository;

    public ProgressOverviewResponse getOverview(Long userId, Integer year, Integer month) {
        YearMonth ym = resolveYearMonth(year, month);
        achievementEvaluationService.evaluateAndAward(userId);

        LeaderboardResponse leaderboard = attendanceStatsService.getLeaderboard(
                ym.getYear(), ym.getMonthValue(), userId);

        long userMinutes = dailySummaryRepository.sumMinutesForUserInRange(
                userId, ym.atDay(1), ym.atEndOfMonth());

        UserAchievementsResponse achievements = achievementEvaluationService.getUserAchievements(userId);

        return ProgressOverviewResponse.builder()
                .year(ym.getYear())
                .month(ym.getMonthValue())
                .examCountdown(examTargetService.getMyTarget(userId))
                .compareAverage(buildCompareAverage(userMinutes, ym))
                .rankInsight(buildRankInsight(leaderboard, userMinutes))
                .latestMilestone(buildLatestMilestone(userId))
                .earnedBadgeCount(achievements.getEarnedCount())
                .totalBadgeCount(achievements.getTotalCount())
                .build();
    }

    public CompareAverageResponse buildCompareAverage(long userMinutes, YearMonth ym) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<User> members = userRepository.findAllByRole(User.Role.MEMBER);
        if (members.isEmpty()) {
            return CompareAverageResponse.builder()
                    .yourHours(roundHours(userMinutes))
                    .libraryAverageHours(0)
                    .differenceHours(roundHours(userMinutes))
                    .message("Keep studying — you're building momentum!")
                    .build();
        }

        long totalMinutes = 0;
        for (User member : members) {
            totalMinutes += dailySummaryRepository.sumMinutesForUserInRange(member.getId(), start, end);
        }
        double avgMinutes = (double) totalMinutes / members.size();
        double yourHours = roundHours(userMinutes);
        double avgHours = roundHours((long) avgMinutes);
        double diff = Math.round((yourHours - avgHours) * 10.0) / 10.0;

        String message = diff > 0
                ? "You're " + diff + "h above the library average — great work!"
                : diff < 0
                    ? "You're " + Math.abs(diff) + "h below average — a little push will get you there!"
                    : "You're right at the library average — keep it up!";

        return CompareAverageResponse.builder()
                .yourHours(yourHours)
                .libraryAverageHours(avgHours)
                .differenceHours(diff)
                .message(message)
                .build();
    }

    public RankInsightResponse buildRankInsight(LeaderboardResponse leaderboard, long userMinutes) {
        Integer currentRank = leaderboard.getCurrentUserRank();
        if (currentRank == null || leaderboard.getEntries().isEmpty()) {
            return RankInsightResponse.builder()
                    .message("Start studying to appear on the leaderboard!")
                    .atTop(false)
                    .yourHours(roundHours(userMinutes))
                    .build();
        }

        if (currentRank == 1) {
            return RankInsightResponse.builder()
                    .currentRank(1)
                    .targetRank(1)
                    .yourHours(roundHours(userMinutes))
                    .targetRankHours(roundHours(userMinutes))
                    .hoursNeeded(0)
                    .message("You're #1 this month — keep leading!")
                    .atTop(true)
                    .build();
        }

        LeaderboardEntryResponse targetEntry = leaderboard.getEntries().stream()
                .filter(e -> e.getRank() == currentRank - 1)
                .findFirst()
                .orElse(null);

        if (targetEntry == null) {
            return RankInsightResponse.builder()
                    .currentRank(currentRank)
                    .yourHours(roundHours(userMinutes))
                    .message("Keep going — every hour counts!")
                    .atTop(false)
                    .build();
        }

        long minutesNeeded = Math.max(0, targetEntry.getTotalMinutes() - userMinutes + 1);
        double hoursNeeded = roundHours(minutesNeeded);

        return RankInsightResponse.builder()
                .currentRank(currentRank)
                .targetRank(targetEntry.getRank())
                .yourHours(roundHours(userMinutes))
                .targetRankHours(targetEntry.getTotalHours())
                .hoursNeeded(hoursNeeded)
                .message("You need " + formatHours(hoursNeeded) + " more to reach Rank #" + targetEntry.getRank())
                .atTop(false)
                .build();
    }

    private AchievementUnlockedResponse buildLatestMilestone(Long userId) {
        return userAchievementRepository.findFirstByUserIdOrderByEarnedAtDesc(userId)
                .map(this::toUnlocked)
                .orElse(null);
    }

    private AchievementUnlockedResponse toUnlocked(UserAchievement ua) {
        var def = ua.getAchievementDefinition();
        return AchievementUnlockedResponse.builder()
                .code(def.getCode())
                .title(def.getTitle())
                .description(def.getDescription())
                .category(def.getCategory())
                .iconKey(def.getIconKey())
                .earnedAt(ua.getEarnedAt() != null ? ua.getEarnedAt().toString() : null)
                .build();
    }

    private String formatHours(double hours) {
        if (hours == Math.floor(hours)) {
            return (int) hours + "h";
        }
        return hours + "h";
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
