package com.library.library_backend.whatsapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.library_backend.whatsapp.config.WhatsAppProperties;
import com.library.library_backend.whatsapp.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class WhatsAppDashboardService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final JdbcTemplate jdbcTemplate;
    private final WhatsAppProperties properties;
    private final WhatsAppService whatsAppService;
    private final ObjectMapper objectMapper;

    public WhatsAppDashboardService(JdbcTemplate jdbcTemplate,
                                    WhatsAppProperties properties,
                                    WhatsAppService whatsAppService,
                                    ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.whatsAppService = whatsAppService;
        this.objectMapper = objectMapper;
    }

    public WhatsAppDashboardResponse getDashboardData(
            int page,
            int pageSize,
            String search,
            String status,
            String templateName
    ) {
        String scope = properties.getScopeKey();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safeSize;

        StringBuilder baseQuery = new StringBuilder("""
                FROM whatsapp_messages wm
                LEFT JOIN users u ON wm.student_id = u.id
                WHERE wm.org_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(scope);

        if (search != null && !search.isBlank()) {
            baseQuery.append("""
                     AND (
                       u.full_name ILIKE ?
                       OR u.member_id ILIKE ?
                       OR wm.recipient_phone ILIKE ?
                       OR wm.recipient_name ILIKE ?
                     )
                    """);
            String pattern = "%" + search.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (status != null && !status.isBlank()) {
            baseQuery.append(" AND wm.message_status = ?");
            params.add(status.trim().toLowerCase());
        }
        if (templateName != null && !templateName.isBlank()) {
            baseQuery.append(" AND wm.template_name = ?");
            params.add(templateName.trim());
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + baseQuery,
                Long.class,
                params.toArray()
        );
        long totalCount = total != null ? total : 0L;

        List<Object> listParams = new ArrayList<>(params);
        listParams.add(safeSize);
        listParams.add(offset);

        String selectQuery = """
                SELECT
                  wm.id,
                  wm.message_id,
                  wm.recipient_phone,
                  wm.template_name,
                  wm.message_status,
                  wm.failure_reason,
                  wm.sent_at,
                  wm.delivered_at,
                  wm.read_at,
                  wm.student_id,
                  wm.variables,
                  u.full_name AS member_name,
                  u.member_id AS member_member_id
                """ + baseQuery + """
                 ORDER BY wm.sent_at DESC NULLS LAST, wm.id DESC
                 LIMIT ? OFFSET ?
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectQuery, listParams.toArray());
        List<WhatsAppDashboardMessage> messages = rows.stream().map(this::mapMessage).toList();

        WhatsAppDashboardStats stats = fetchStats(scope);
        int totalPages = safeSize > 0 ? (int) Math.ceil((double) totalCount / safeSize) : 0;

        return new WhatsAppDashboardResponse(
                messages,
                stats,
                new WhatsAppDashboardPagination(safePage, safeSize, totalCount, totalPages)
        );
    }

    public void retryFailedMessage(long messageDbId) {
        String scope = properties.getScopeKey();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT wm.*, u.full_name AS member_name
                FROM whatsapp_messages wm
                LEFT JOIN users u ON wm.student_id = u.id
                WHERE wm.id = ? AND wm.org_id = ? AND wm.message_status = 'failed'
                """,
                messageDbId,
                scope
        );

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Failed message not found");
        }

        Map<String, Object> row = rows.get(0);
        String templateName = String.valueOf(row.get("template_name"));
        String templateLanguage = row.get("template_language") != null
                ? String.valueOf(row.get("template_language"))
                : LibraryAdmissionNotificationService.TEMPLATE_LANGUAGE;
        String phone = String.valueOf(row.get("recipient_phone"));
        Long recipientId = row.get("student_id") != null
                ? ((Number) row.get("student_id")).longValue()
                : null;

        Map<String, Object> variables = parseVariables(row.get("variables"));
        if (variables.isEmpty()) {
            variables = rebuildVariables(templateName, row);
        }

        try {
            whatsAppService.retryExistingMessage(
                    messageDbId,
                    phone,
                    templateName,
                    templateLanguage,
                    variables,
                    scope,
                    recipientId
            );
        } catch (Exception e) {
            jdbcTemplate.update(
                    """
                    UPDATE whatsapp_messages
                    SET failure_reason = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ? AND org_id = ?
                    """,
                    e.getMessage(),
                    messageDbId,
                    scope
            );
            throw new IllegalStateException("Failed to retry message: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> rebuildVariables(String templateName, Map<String, Object> row) {
        Map<String, Object> variables = new LinkedHashMap<>();
        if (LibraryAdmissionNotificationService.TEMPLATE_NAME.equals(templateName)) {
            String name = row.get("member_name") != null
                    ? String.valueOf(row.get("member_name"))
                    : row.get("recipient_name") != null
                    ? String.valueOf(row.get("recipient_name"))
                    : "Member";
            variables.put("1", name.trim());
        }
        return variables;
    }

    private WhatsAppDashboardStats fetchStats(String scope) {
        LocalDate todayIst = LocalDate.now(IST);

        Map<String, Object> raw = jdbcTemplate.queryForMap(
                """
                SELECT
                  COUNT(*) AS total,
                  COUNT(CASE WHEN message_status IN ('sent', 'delivered', 'read') THEN 1 END) AS combined_success,
                  COUNT(CASE WHEN message_status = 'sent' THEN 1 END) AS sent,
                  COUNT(CASE WHEN message_status = 'delivered' THEN 1 END) AS delivered,
                  COUNT(CASE WHEN message_status = 'read' THEN 1 END) AS read,
                  COUNT(CASE WHEN message_status = 'failed' THEN 1 END) AS failed,
                  COUNT(CASE WHEN (sent_at AT TIME ZONE 'Asia/Kolkata')::date = ? THEN 1 END) AS today_messages
                FROM whatsapp_messages
                WHERE org_id = ?
                """,
                todayIst,
                scope
        );

        Map<String, Object> queueRaw = jdbcTemplate.queryForMap(
                """
                SELECT
                  COUNT(CASE WHEN status = 'pending' THEN 1 END) AS pending,
                  COUNT(CASE WHEN status = 'failed' THEN 1 END) AS failed
                FROM whatsapp_message_queue
                WHERE org_id = ?
                """,
                scope
        );

        long total = toLong(raw.get("total"));
        long successful = toLong(raw.get("combined_success"));
        int successRate = total > 0 ? (int) Math.round((successful * 100.0) / total) : 0;

        return new WhatsAppDashboardStats(
                total,
                successful,
                toLong(raw.get("sent")),
                toLong(raw.get("delivered")),
                toLong(raw.get("read")),
                toLong(raw.get("failed")),
                toLong(raw.get("today_messages")),
                successRate,
                toLong(queueRaw.get("pending")),
                toLong(queueRaw.get("failed"))
        );
    }

    private WhatsAppDashboardMessage mapMessage(Map<String, Object> row) {
        JsonNode variablesNode = null;
        Object rawVars = row.get("variables");
        if (rawVars != null) {
            try {
                variablesNode = objectMapper.readTree(String.valueOf(rawVars));
            } catch (Exception ignored) {
                // leave null
            }
        }

        return new WhatsAppDashboardMessage(
                ((Number) row.get("id")).longValue(),
                row.get("message_id") != null ? String.valueOf(row.get("message_id")) : null,
                String.valueOf(row.get("recipient_phone")),
                String.valueOf(row.get("template_name")),
                String.valueOf(row.get("message_status")),
                row.get("failure_reason") != null ? String.valueOf(row.get("failure_reason")) : null,
                toInstant(row.get("sent_at")),
                toInstant(row.get("delivered_at")),
                toInstant(row.get("read_at")),
                row.get("student_id") != null ? ((Number) row.get("student_id")).longValue() : null,
                row.get("member_name") != null ? String.valueOf(row.get("member_name")) : null,
                row.get("member_member_id") != null ? String.valueOf(row.get("member_member_id")) : null,
                variablesNode
        );
    }

    private Map<String, Object> parseVariables(Object raw) {
        if (raw == null) {
            return Map.of();
        }
        try {
            if (raw instanceof Map<?, ?> map) {
                Map<String, Object> out = new LinkedHashMap<>();
                map.forEach((k, v) -> out.put(String.valueOf(k), v));
                return out;
            }
            return objectMapper.readValue(String.valueOf(raw), new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof java.util.Date d) {
            return d.toInstant();
        }
        return null;
    }
}
