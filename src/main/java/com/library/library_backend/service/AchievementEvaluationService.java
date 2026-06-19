package com.library.library_backend.service;

import com.library.library_backend.dto.AchievementProgressResponse;
import com.library.library_backend.dto.AchievementUnlockedResponse;
import com.library.library_backend.dto.UserAchievementsResponse;
import com.library.library_backend.model.AchievementDefinition;
import com.library.library_backend.model.User;
import com.library.library_backend.model.UserAchievement;
import com.library.library_backend.repository.AchievementDefinitionRepository;
import com.library.library_backend.repository.AttendanceRepository;
import com.library.library_backend.repository.UserAchievementRepository;
import com.library.library_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AchievementEvaluationService {

    private static final int EARLY_BIRD_MINUTES = 600;  // 10:00 AM
    private static final int NIGHT_OWL_MINUTES = 1140;  // 19:00 (7 PM)

    @Autowired
    private AchievementDefinitionRepository definitionRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMetricsService userMetricsService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Transactional
    public List<AchievementUnlockedResponse> evaluateAndAward(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserMetricsService.UserMetrics metrics = userMetricsService.getMetrics(
                userId, YearMonth.now(java.time.ZoneId.of("Asia/Kolkata")));

        Set<Long> earnedIds = userAchievementRepository.findEarnedDefinitionIdsByUserId(userId);
        List<AchievementDefinition> definitions = definitionRepository.findAllByIsActiveTrueOrderBySortOrderAsc();
        List<AchievementUnlockedResponse> newlyUnlocked = new ArrayList<>();

        for (AchievementDefinition def : definitions) {
            if (earnedIds.contains(def.getId())) {
                continue;
            }
            if (isThresholdMet(userId, def, metrics)) {
                UserAchievement ua = userAchievementRepository.save(UserAchievement.builder()
                        .user(user)
                        .achievementDefinition(def)
                        .build());
                newlyUnlocked.add(toUnlockedResponse(ua));
            }
        }
        return newlyUnlocked;
    }

    public UserAchievementsResponse getUserAchievements(Long userId) {
        evaluateAndAward(userId);

        UserMetricsService.UserMetrics metrics = userMetricsService.getMetrics(
                userId, YearMonth.now(java.time.ZoneId.of("Asia/Kolkata")));

        List<AchievementDefinition> definitions = definitionRepository.findAllByIsActiveTrueOrderBySortOrderAsc();
        Map<Long, UserAchievement> earnedMap = userAchievementRepository.findByUserIdOrderByEarnedAtDesc(userId)
                .stream()
                .collect(Collectors.toMap(
                        ua -> ua.getAchievementDefinition().getId(),
                        ua -> ua,
                        (a, b) -> a));

        List<AchievementProgressResponse> progressList = new ArrayList<>();
        int earnedCount = 0;
        for (AchievementDefinition def : definitions) {
            UserAchievement earned = earnedMap.get(def.getId());
            int currentValue = currentValueForDefinition(userId, def, metrics);
            int progressPercent = computeProgressPercent(def, currentValue);
            boolean isEarned = earned != null;
            if (isEarned) {
                earnedCount++;
            }
            progressList.add(AchievementProgressResponse.builder()
                    .id(def.getId())
                    .code(def.getCode())
                    .category(def.getCategory())
                    .title(def.getTitle())
                    .description(def.getDescription())
                    .iconKey(def.getIconKey())
                    .thresholdValue(def.getThresholdValue())
                    .thresholdUnit(def.getThresholdUnit())
                    .sortOrder(def.getSortOrder())
                    .earned(isEarned)
                    .earnedAt(isEarned && earned.getEarnedAt() != null ? earned.getEarnedAt().toString() : null)
                    .progressPercent(isEarned ? 100 : progressPercent)
                    .currentValue(currentValue)
                    .build());
        }

        return UserAchievementsResponse.builder()
                .earnedCount(earnedCount)
                .totalCount(definitions.size())
                .currentStreak(metrics.currentStreak())
                .longestStreak(metrics.longestStreak())
                .achievements(progressList)
                .build();
    }

    private boolean isThresholdMet(Long userId, AchievementDefinition def, UserMetricsService.UserMetrics metrics) {
        return switch (def.getThresholdUnit()) {
            case "STREAK_DAYS" -> metrics.currentStreak() >= def.getThresholdValue()
                    || metrics.longestStreak() >= def.getThresholdValue();
            case "MINUTES" -> metrics.lifetimeMinutes() >= def.getThresholdValue();
            case "DAYS" -> metrics.lifetimeDaysPresent() >= def.getThresholdValue();
            case "TIME_BEFORE" -> attendanceRepository.hasCheckInBeforeMinutesOfDay(userId, def.getThresholdValue());
            case "TIME_AFTER" -> attendanceRepository.hasCheckInAtOrAfterMinutesOfDay(userId, def.getThresholdValue());
            default -> false;
        };
    }

    private int currentValueForDefinition(Long userId, AchievementDefinition def, UserMetricsService.UserMetrics metrics) {
        return switch (def.getThresholdUnit()) {
            case "STREAK_DAYS" -> Math.max(metrics.currentStreak(), metrics.longestStreak());
            case "MINUTES" -> (int) Math.min(metrics.lifetimeMinutes(), Integer.MAX_VALUE);
            case "DAYS" -> metrics.lifetimeDaysPresent();
            case "TIME_BEFORE", "TIME_AFTER" -> {
                boolean met = "TIME_BEFORE".equals(def.getThresholdUnit())
                        ? attendanceRepository.hasCheckInBeforeMinutesOfDay(userId, def.getThresholdValue())
                        : attendanceRepository.hasCheckInAtOrAfterMinutesOfDay(userId, def.getThresholdValue());
                yield met ? def.getThresholdValue() : 0;
            }
            default -> 0;
        };
    }

    private int computeProgressPercent(AchievementDefinition def, int currentValue) {
        if ("TIME_BEFORE".equals(def.getThresholdUnit()) || "TIME_AFTER".equals(def.getThresholdUnit())) {
            return currentValue > 0 ? 100 : 0;
        }
        if (def.getThresholdValue() <= 0) {
            return 0;
        }
        return Math.min(100, (int) Math.round((currentValue * 100.0) / def.getThresholdValue()));
    }

    private AchievementUnlockedResponse toUnlockedResponse(UserAchievement ua) {
        AchievementDefinition def = ua.getAchievementDefinition();
        return AchievementUnlockedResponse.builder()
                .code(def.getCode())
                .title(def.getTitle())
                .description(def.getDescription())
                .category(def.getCategory())
                .iconKey(def.getIconKey())
                .earnedAt(ua.getEarnedAt() != null ? ua.getEarnedAt().toString() : null)
                .build();
    }
}
