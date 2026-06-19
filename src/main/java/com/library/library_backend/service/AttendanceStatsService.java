package com.library.library_backend.service;

import com.library.library_backend.dto.LeaderboardEntryResponse;
import com.library.library_backend.dto.LeaderboardResponse;
import com.library.library_backend.dto.MonthlyAttendanceStatsResponse;
import com.library.library_backend.model.Attendance;
import com.library.library_backend.model.DailyAttendanceSummary;
import com.library.library_backend.model.User;
import com.library.library_backend.repository.DailyAttendanceSummaryRepository;
import com.library.library_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceStatsService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private DailyAttendanceSummaryRepository dailySummaryRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void recordSessionCompletion(Attendance attendance) {
        if (attendance.getCheckOutTime() == null || attendance.getCheckInTime() == null) {
            return;
        }

        LocalDate sessionDate = attendance.getCheckInTime().atZoneSameInstant(LIBRARY_ZONE).toLocalDate();
        long sessionMinutes = ChronoUnit.MINUTES.between(attendance.getCheckInTime(), attendance.getCheckOutTime());
        if (sessionMinutes < 0) {
            sessionMinutes = 0;
        }

        DailyAttendanceSummary summary = dailySummaryRepository
                .findByUserIdAndAttendanceDate(attendance.getUser().getId(), sessionDate)
                .orElseGet(() -> DailyAttendanceSummary.builder()
                        .user(attendance.getUser())
                        .attendanceDate(sessionDate)
                        .totalMinutes(0)
                        .build());

        summary.setTotalMinutes(summary.getTotalMinutes() + (int) sessionMinutes);
        summary.setLastPunchOutTime(attendance.getCheckOutTime());
        dailySummaryRepository.save(summary);
    }

    public MonthlyAttendanceStatsResponse getMonthlyStats(Long userId, Integer year, Integer month) {
        YearMonth ym = resolveYearMonth(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        long totalMinutes = dailySummaryRepository.sumMinutesForUserInRange(userId, start, end);
        int daysPresent = dailySummaryRepository.countPresentDaysForUserInRange(userId, start, end);

        LeaderboardResponse leaderboard = buildLeaderboard(ym.getYear(), ym.getMonthValue(), userId);
        Integer rank = leaderboard.getCurrentUserRank();

        return MonthlyAttendanceStatsResponse.builder()
                .year(ym.getYear())
                .month(ym.getMonthValue())
                .daysPresent(daysPresent)
                .totalMinutes(totalMinutes)
                .totalHours(roundHours(totalMinutes))
                .rank(rank)
                .totalStudents(leaderboard.getEntries().size())
                .build();
    }

    public LeaderboardResponse getLeaderboard(Integer year, Integer month, Long currentUserId) {
        YearMonth ym = resolveYearMonth(year, month);
        return buildLeaderboard(ym.getYear(), ym.getMonthValue(), currentUserId);
    }

    private LeaderboardResponse buildLeaderboard(int year, int month, Long currentUserId) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<User> members = userRepository.findAllByRole(User.Role.MEMBER);
        Map<Long, long[]> statsByUser = new HashMap<>();

        for (Object[] row : dailySummaryRepository.aggregateMinutesByUserInRange(start, end)) {
            Long userId = (Long) row[0];
            long minutes = ((Number) row[1]).longValue();
            long days = ((Number) row[2]).longValue();
            statsByUser.put(userId, new long[]{minutes, days});
        }

        List<LeaderboardEntryResponse> entries = new ArrayList<>();
        for (User member : members) {
            long[] stats = statsByUser.getOrDefault(member.getId(), new long[]{0L, 0L});
            entries.add(LeaderboardEntryResponse.builder()
                    .userId(member.getId())
                    .memberId(member.getMemberId())
                    .fullName(member.getFullName())
                    .daysPresent((int) stats[1])
                    .totalMinutes(stats[0])
                    .totalHours(roundHours(stats[0]))
                    .currentUser(currentUserId != null && currentUserId.equals(member.getId()))
                    .build());
        }

        entries.sort(Comparator
                .comparingLong(LeaderboardEntryResponse::getTotalMinutes).reversed()
                .thenComparing(Comparator.comparingInt(LeaderboardEntryResponse::getDaysPresent).reversed())
                .thenComparing(e -> e.getFullName().toLowerCase()));

        Integer currentUserRank = null;
        for (int i = 0; i < entries.size(); i++) {
            int rank = i + 1;
            LeaderboardEntryResponse entry = entries.get(i);
            entry.setRank(rank);
            entry.setBadge(badgeForRank(rank));
            if (entry.isCurrentUser()) {
                currentUserRank = rank;
            }
        }

        return LeaderboardResponse.builder()
                .year(year)
                .month(month)
                .entries(entries)
                .currentUserRank(currentUserRank)
                .build();
    }

    private String badgeForRank(int rank) {
        return switch (rank) {
            case 1 -> "GOLD";
            case 2 -> "SILVER";
            case 3 -> "BRONZE";
            default -> null;
        };
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
