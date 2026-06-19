package com.library.library_backend.service;

import com.library.library_backend.dto.StudyLogEntryResponse;
import com.library.library_backend.dto.StudyLogHistoryResponse;
import com.library.library_backend.dto.StudyLogRequest;
import com.library.library_backend.model.DailyStudyLog;
import com.library.library_backend.model.User;
import com.library.library_backend.repository.DailyStudyLogRepository;
import com.library.library_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudyLogService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private DailyStudyLogRepository studyLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public StudyLogEntryResponse addLog(Long userId, StudyLogRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDate logDate = request.getLogDate() != null && !request.getLogDate().isBlank()
                ? LocalDate.parse(request.getLogDate())
                : LocalDate.now(LIBRARY_ZONE);

        int minutes = (int) Math.round(request.getHoursStudied() * 60);
        if (minutes < 1) {
            minutes = 1;
        }

        DailyStudyLog saved = studyLogRepository.save(DailyStudyLog.builder()
                .user(user)
                .logDate(logDate)
                .subject(request.getSubject().trim())
                .minutesStudied(minutes)
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build());

        return toResponse(saved);
    }

    public StudyLogHistoryResponse getHistory(Long userId, Integer year, Integer month) {
        YearMonth ym = resolveYearMonth(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<DailyStudyLog> logs = studyLogRepository.findByUserAndDateRange(userId, start, end);
        long totalMinutes = studyLogRepository.sumMinutesForUserInRange(userId, start, end);

        return StudyLogHistoryResponse.builder()
                .year(ym.getYear())
                .month(ym.getMonthValue())
                .totalHoursLogged(roundHours(totalMinutes))
                .entries(logs.stream().map(this::toResponse).collect(Collectors.toList()))
                .build();
    }

    private StudyLogEntryResponse toResponse(DailyStudyLog log) {
        return StudyLogEntryResponse.builder()
                .id(log.getId())
                .logDate(log.getLogDate().toString())
                .subject(log.getSubject())
                .minutesStudied(log.getMinutesStudied())
                .hoursStudied(roundHours(log.getMinutesStudied()))
                .notes(log.getNotes())
                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt().toString() : null)
                .build();
    }

    private double roundHours(long minutes) {
        return Math.round(minutes / 6.0) / 10.0;
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        if (year != null && month != null) {
            return YearMonth.of(year, month);
        }
        return YearMonth.now(LIBRARY_ZONE);
    }
}
