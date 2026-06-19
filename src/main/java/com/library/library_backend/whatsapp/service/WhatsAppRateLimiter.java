package com.library.library_backend.whatsapp.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class WhatsAppRateLimiter {

    private final JdbcTemplate jdbcTemplate;
    private final int rateLimitPerSecond;
    private final int rateLimitPerDay;

    public WhatsAppRateLimiter(JdbcTemplate jdbcTemplate,
                               com.library.library_backend.whatsapp.config.WhatsAppProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.rateLimitPerSecond = properties.getRateLimitPerSecond();
        this.rateLimitPerDay = properties.getRateLimitPerDay();
    }

    public void enforceRateLimit(String orgId) {
        Instant now = Instant.now();
        Timestamp oneSecondAgo = Timestamp.from(now.minus(1, ChronoUnit.SECONDS));
        Timestamp oneDayAgo = Timestamp.from(now.minus(24, ChronoUnit.HOURS));

        Integer recentCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM whatsapp_messages
                WHERE org_id = ? AND sent_at > ?
                """,
                Integer.class,
                orgId,
                oneSecondAgo
        );
        if (recentCount != null && recentCount >= rateLimitPerSecond) {
            throw new IllegalStateException("Rate limit exceeded: Too many messages per second");
        }

        Integer dailyCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM whatsapp_messages
                WHERE org_id = ? AND sent_at > ?
                """,
                Integer.class,
                orgId,
                oneDayAgo
        );
        if (dailyCount != null && dailyCount >= rateLimitPerDay) {
            throw new IllegalStateException("Rate limit exceeded: Too many messages per day");
        }
    }

    public int getRateLimitPerDay() {
        return rateLimitPerDay;
    }

    public int getRateLimitPerSecond() {
        return rateLimitPerSecond;
    }
}
