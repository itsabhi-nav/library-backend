package com.library.library_backend.whatsapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.library_backend.whatsapp.config.WhatsAppProperties;
import com.library.library_backend.whatsapp.dto.BulkMessagingOptions;
import com.library.library_backend.whatsapp.dto.QueueProcessResult;
import com.library.library_backend.whatsapp.dto.SendMessageCallback;
import com.library.library_backend.whatsapp.dto.WhatsAppRecipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
public class WhatsAppQueueService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppQueueService.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final int rateLimitPerDay;
    private final int rateLimitPerSecond;

    private static volatile boolean isProcessing = false;
    private static final Map<String, Consumer<BulkMessagingOptions.BatchCompletionResult>> COMPLETION_CALLBACKS =
            new ConcurrentHashMap<>();

    public WhatsAppQueueService(JdbcTemplate jdbcTemplate,
                              ObjectMapper objectMapper,
                              WhatsAppProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.rateLimitPerDay = properties.getRateLimitPerDay();
        this.rateLimitPerSecond = properties.getRateLimitPerSecond();
    }

    public record QueueEnqueueResult(int queuedCount, String batchId) {}

    public QueueEnqueueResult addMessagesToQueue(
            List<WhatsAppRecipient> recipients,
            String templateName,
            String templateLanguage,
            String orgId,
            String batchId,
            Map<String, Object> metadata,
            int priority
    ) {
        Timestamp scheduleTime = Timestamp.from(Instant.now());
        if (metadata != null && metadata.get("scheduledFor") != null) {
            scheduleTime = Timestamp.from(Instant.parse(String.valueOf(metadata.get("scheduledFor"))));
        }

        String metadataJson = toJson(metadata != null ? metadata : Map.of());
        for (WhatsAppRecipient recipient : recipients) {
            jdbcTemplate.update(
                    """
                    INSERT INTO whatsapp_message_queue (
                        org_id, template_name, template_language, phone_number,
                        recipient_name, recipient_id, variables, scheduled_for,
                        batch_id, metadata, priority
                    ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?::jsonb, ?)
                    """,
                    orgId,
                    templateName,
                    templateLanguage,
                    recipient.getPhoneNumber(),
                    recipient.getName(),
                    recipient.getId(),
                    toJson(recipient.getVariables()),
                    scheduleTime,
                    batchId,
                    metadataJson,
                    priority
            );
        }

        if (recipients.size() > 1) {
            log.info("Messages queued: orgId={}, batchId={}, count={}", orgId, batchId, recipients.size());
        }
        return new QueueEnqueueResult(recipients.size(), batchId);
    }

    public void nudgeProcessor(
            SendMessageCallback callback,
            String batchId,
            Consumer<BulkMessagingOptions.BatchCompletionResult> onComplete
    ) {
        if (batchId != null && onComplete != null) {
            COMPLETION_CALLBACKS.put(batchId, onComplete);
        }
        if (isProcessing) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                processQueuedMessages(callback, null, batchId);
            } catch (Exception e) {
                log.error("Background queue processing failed: {}", e.getMessage(), e);
            }
        });
    }

    public QueueProcessResult processQueuedMessages(
            SendMessageCallback callback,
            String orgId,
            String batchId
    ) {
        if (isProcessing) {
            log.warn("Queue processing already in progress, skipping start");
            return QueueProcessResult.empty();
        }

        Set<String> processedBatchIds = new HashSet<>();
        Map<String, BulkMessagingOptions.BatchCompletionResult> batchResults = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int totalSent = 0;
        int totalFailed = 0;
        int totalProcessed = 0;

        if (batchId != null) {
            processedBatchIds.add(batchId);
        }

        isProcessing = true;
        try {
            LocalDate todayIst = LocalDate.now(IST);
            Timestamp dayStart = Timestamp.from(todayIst.atStartOfDay(IST).toInstant());

            while (true) {
                List<Map<String, Object>> batchMessages = fetchPendingBatch(orgId, batchId);
                if (batchMessages.isEmpty()) {
                    break;
                }

                log.info("Processing queue batch: size={}, processedSoFar={}", batchMessages.size(), totalProcessed);

                String countOrg = orgId != null ? orgId : String.valueOf(batchMessages.get(0).get("org_id"));
                Integer dailyCount = jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*) FROM whatsapp_messages
                        WHERE org_id = ? AND sent_at > ?
                        """,
                        Integer.class,
                        countOrg,
                        dayStart
                );
                int currentDailyCount = dailyCount != null ? dailyCount : 0;

                for (int i = 0; i < batchMessages.size(); i++) {
                    Map<String, Object> message = batchMessages.get(i);
                    String messageOrgId = String.valueOf(message.get("org_id"));
                    String messageBatchId = message.get("batch_id") != null ? String.valueOf(message.get("batch_id")) : null;

                    if (messageBatchId != null) {
                        processedBatchIds.add(messageBatchId);
                    }

                    if (currentDailyCount >= rateLimitPerDay) {
                        log.warn("Daily limit reached for orgId={}, count={}", messageOrgId, currentDailyCount);
                        return new QueueProcessResult(totalProcessed, totalSent, totalFailed, errors);
                    }

                    try {
                        Map<String, Object> metadata = parseJsonMap(message.get("metadata"));
                        Map<String, Object> variables = parseJsonMap(message.get("variables"));

                        callback.send(
                                String.valueOf(message.get("phone_number")),
                                String.valueOf(message.get("template_name")),
                                String.valueOf(message.get("template_language")),
                                variables,
                                messageOrgId,
                                metadata.get("mediaId") != null ? String.valueOf(metadata.get("mediaId")) : null,
                                true,
                                message.get("recipient_id") != null ? ((Number) message.get("recipient_id")).longValue() : null,
                                metadata.get("documentFilename") != null ? String.valueOf(metadata.get("documentFilename")) : null
                        );

                        jdbcTemplate.update(
                                """
                                UPDATE whatsapp_message_queue
                                SET status = 'sent', sent_at = CURRENT_TIMESTAMP
                                WHERE id = ?
                                """,
                                message.get("id")
                        );

                        totalSent++;
                        currentDailyCount++;
                        totalProcessed++;

                        if (messageBatchId != null) {
                            BulkMessagingOptions.BatchCompletionResult prev = batchResults.getOrDefault(
                                    messageBatchId, new BulkMessagingOptions.BatchCompletionResult(0, 0));
                            batchResults.put(messageBatchId,
                                    new BulkMessagingOptions.BatchCompletionResult(prev.sentCount() + 1, prev.failedCount()));
                        }

                        long delay = Math.max(10, 1000 / rateLimitPerSecond);
                        if (i > 0 && String.valueOf(batchMessages.get(i - 1).get("phone_number"))
                                .equals(String.valueOf(message.get("phone_number")))) {
                            delay = 1000;
                        }
                        Thread.sleep(delay);
                    } catch (Exception e) {
                        totalFailed++;
                        totalProcessed++;
                        errors.add("Message " + message.get("id") + ": " + e.getMessage());

                        if (messageBatchId != null) {
                            BulkMessagingOptions.BatchCompletionResult prev = batchResults.getOrDefault(
                                    messageBatchId, new BulkMessagingOptions.BatchCompletionResult(0, 0));
                            batchResults.put(messageBatchId,
                                    new BulkMessagingOptions.BatchCompletionResult(prev.sentCount(), prev.failedCount() + 1));
                        }

                        int newRetry = message.get("current_retries") != null
                                ? ((Number) message.get("current_retries")).intValue() + 1 : 1;
                        jdbcTemplate.update(
                                """
                                UPDATE whatsapp_message_queue
                                SET status = 'failed', error_message = ?, current_retries = ?
                                WHERE id = ?
                                """,
                                e.getMessage(),
                                newRetry,
                                message.get("id")
                        );
                    }
                }

                Thread.sleep(1000);
            }

            log.info("Queue processing completed: processed={}, sent={}, failed={}",
                    totalProcessed, totalSent, totalFailed);

            for (String bId : processedBatchIds) {
                Consumer<BulkMessagingOptions.BatchCompletionResult> cb = COMPLETION_CALLBACKS.get(bId);
                if (cb == null) {
                    continue;
                }
                Integer remaining = jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*) FROM whatsapp_message_queue
                        WHERE batch_id = ? AND status = 'pending'
                        """,
                        Integer.class,
                        bId
                );
                if (remaining != null && remaining == 0) {
                    COMPLETION_CALLBACKS.remove(bId);
                    BulkMessagingOptions.BatchCompletionResult result = batchResults.getOrDefault(
                            bId, new BulkMessagingOptions.BatchCompletionResult(0, 0));
                    try {
                        cb.accept(result);
                    } catch (Exception e) {
                        log.error("Batch completion callback failed for batchId={}: {}", bId, e.getMessage());
                    }
                }
            }

            return new QueueProcessResult(totalProcessed, totalSent, totalFailed, errors);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Queue processing interrupted", e);
        } finally {
            isProcessing = false;
        }
    }

    public int cleanupOldMessages(int daysOld) {
        Instant cutoff = Instant.now().atZone(IST).minusDays(daysOld).toInstant();
        int deleted = jdbcTemplate.update(
                """
                DELETE FROM whatsapp_message_queue
                WHERE status IN ('sent', 'failed')
                AND updated_at < ?
                """,
                Timestamp.from(cutoff)
        );
        log.info("Cleaned up {} old queue messages (retention {} days)", deleted, daysOld);
        return deleted;
    }

    public void resetFailedMessagesForRetry() {
        int updated = jdbcTemplate.update(
                """
                UPDATE whatsapp_message_queue
                SET status = 'pending', scheduled_for = NOW()
                WHERE status = 'failed'
                AND current_retries <= max_retries
                """
        );
        log.info("Reset {} failed queue messages for retry", updated);
    }

    private List<Map<String, Object>> fetchPendingBatch(String orgId, String batchId) {
        StringBuilder sql = new StringBuilder("""
                SELECT * FROM whatsapp_message_queue
                WHERE status = 'pending'
                AND scheduled_for <= ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(Timestamp.from(Instant.now()));

        if (orgId != null) {
            sql.append(" AND org_id = ?");
            params.add(orgId);
        }
        if (batchId != null) {
            sql.append(" AND batch_id = ?");
            params.add(batchId);
        }
        sql.append(" ORDER BY priority ASC, created_at ASC LIMIT 100");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    private Map<String, Object> parseJsonMap(Object raw) {
        if (raw == null) {
            return new HashMap<>();
        }
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> out = new HashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        try {
            return objectMapper.readValue(String.valueOf(raw), new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static String newBatchId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
