package com.library.library_backend.service;

import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class StreakCalculator {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Kolkata");

    public record StreakResult(int currentStreak, int longestStreak) {}

    public StreakResult calculate(List<LocalDate> presentDates) {
        if (presentDates == null || presentDates.isEmpty()) {
            return new StreakResult(0, 0);
        }

        List<LocalDate> sorted = new ArrayList<>(presentDates);
        Collections.sort(sorted);

        int longest = 1;
        int running = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                running++;
                longest = Math.max(longest, running);
            } else if (!sorted.get(i).equals(sorted.get(i - 1))) {
                running = 1;
            }
        }

        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        int current = 0;
        if (sorted.contains(today)) {
            current = 1;
            LocalDate cursor = today.minusDays(1);
            while (sorted.contains(cursor)) {
                current++;
                cursor = cursor.minusDays(1);
            }
        } else if (sorted.contains(today.minusDays(1))) {
            current = 1;
            LocalDate cursor = today.minusDays(2);
            while (sorted.contains(cursor)) {
                current++;
                cursor = cursor.minusDays(1);
            }
        }

        return new StreakResult(current, longest);
    }
}
