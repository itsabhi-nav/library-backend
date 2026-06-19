package com.library.library_backend.whatsapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.library_backend.whatsapp.config.WhatsAppProperties;
import com.library.library_backend.whatsapp.dto.BulkMessagingOptions;
import com.library.library_backend.whatsapp.dto.BulkSendResult;
import com.library.library_backend.whatsapp.dto.WhatsAppRecipient;
import com.library.library_backend.whatsapp.support.WhatsAppPhoneFormatter;
import com.library.library_backend.whatsapp.support.WhatsAppTemplateComponentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final WhatsAppProperties properties;
    private final WhatsAppRateLimiter rateLimiter;
    private final WhatsAppQueueService queueService;
    private final WhatsAppBulkMessagingService bulkMessagingService;
    private final WhatsAppTemplateComponentBuilder componentBuilder;
    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private int consecutiveFailures = 0;
    private long lastFailureTime = 0;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final long CIRCUIT_BREAKER_TIMEOUT_MS = 60_000;

    public WhatsAppService(WhatsAppProperties properties,
                           WhatsAppRateLimiter rateLimiter,
                           WhatsAppQueueService queueService,
                           WhatsAppBulkMessagingService bulkMessagingService,
                           JdbcTemplate jdbcTemplate,
                           RestClient whatsAppRestClient,
                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.queueService = queueService;
        this.bulkMessagingService = bulkMessagingService;
        this.jdbcTemplate = jdbcTemplate;
        this.restClient = whatsAppRestClient;
        this.objectMapper = objectMapper;
        this.componentBuilder = new WhatsAppTemplateComponentBuilder(jdbcTemplate);
    }

    public void sendTemplateMessage(
            String to,
            String templateName,
            String templateLanguage,
            Map<String, Object> variables
    ) {
        sendTemplateMessage(to, templateName, templateLanguage, variables,
                properties.getScopeKey(), null, false, null, null);
    }

    public void sendTemplateMessage(
            String to,
            String templateName,
            String templateLanguage,
            Map<String, Object> variables,
            String scopeKey
    ) {
        sendTemplateMessage(to, templateName, templateLanguage, variables, scopeKey, null, false, null, null);
    }

    public void sendTemplateMessage(
            String to,
            String templateName,
            String templateLanguage,
            Map<String, Object> variables,
            String scopeKey,
            String mediaId,
            boolean skipQueue,
            Long recipientId,
            String documentFilename
    ) {
        if (!properties.isEnabled()) {
            log.warn("WhatsApp disabled — skipping send to {} (set WHATSAPP_ENABLED=true)", to);
            return;
        }

        final String scope = scopeKey != null ? scopeKey : properties.getScopeKey();

        if (!skipQueue) {
            String batchId = WhatsAppQueueService.newBatchId("single");
            WhatsAppRecipient recipient = new WhatsAppRecipient();
            recipient.setPhoneNumber(to);
            recipient.setVariables(variables != null ? variables : Map.of());
            recipient.setId(recipientId);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "individual_send");
            if (mediaId != null) {
                metadata.put("mediaId", mediaId);
            }
            if (documentFilename != null) {
                metadata.put("documentFilename", documentFilename);
            }

            queueService.addMessagesToQueue(
                    List.of(recipient),
                    templateName,
                    templateLanguage != null ? templateLanguage : "hi",
                    scope,
                    batchId,
                    metadata,
                    1
            );
            queueService.nudgeProcessor(this::sendFromQueue, batchId, null);
            return;
        }

        String formattedPhone = WhatsAppPhoneFormatter.format(to);
        Map<String, Object> vars = variables != null ? new LinkedHashMap<>(variables) : new LinkedHashMap<>();

        try {
            checkCircuitBreaker();

            if (!validateTemplate(templateName, templateLanguage != null ? templateLanguage : "hi", scope)) {
                throw new IllegalStateException(
                        "Template '" + templateName + "' not found or not approved (scope=" + scope + ")");
            }

            rateLimiter.enforceRateLimit(scope);

            List<Map<String, Object>> components = componentBuilder.build(
                    vars, templateName, templateLanguage != null ? templateLanguage : "en", scope, mediaId, documentFilename);

            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "to", formattedPhone,
                    "type", "template",
                    "template", Map.of(
                            "name", templateName,
                            "language", Map.of("code", templateLanguage != null ? templateLanguage : "hi"),
                            "components", components
                    )
            );

            log.info("Sending WhatsApp template: template={}, phone={}", templateName, formattedPhone);

            String path = "/" + properties.getApiVersion() + "/" + properties.getPhoneNumberId() + "/messages";
            JsonNode response = restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            String lang = templateLanguage != null ? templateLanguage : "hi";
            String variablesJson = toJson(vars);
            String messageId = extractMessageId(response);
            jdbcTemplate.update(
                    """
                    INSERT INTO whatsapp_messages (
                        org_id, recipient_phone, template_name, template_language,
                        message_id, message_status, student_id, variables, sent_at
                    ) VALUES (?, ?, ?, ?, ?, 'sent', ?, ?::jsonb, CURRENT_TIMESTAMP)
                    """,
                    scope,
                    formattedPhone,
                    templateName,
                    lang,
                    messageId,
                    recipientId,
                    variablesJson
            );
            recordApiResult(true);
        } catch (Exception e) {
            recordApiResult(false, e);
            log.error("Failed to send WhatsApp message: to={}, template={}, error={}",
                    formattedPhone, templateName, e.getMessage());
            try {
                String lang = templateLanguage != null ? templateLanguage : "hi";
                jdbcTemplate.update(
                        """
                        INSERT INTO whatsapp_messages (
                            org_id, recipient_phone, template_name, template_language,
                            message_id, message_status, failure_reason, student_id, variables, sent_at
                        ) VALUES (?, ?, ?, ?, ?, 'failed', ?, ?, ?::jsonb, CURRENT_TIMESTAMP)
                        """,
                        scope,
                        formattedPhone,
                        templateName,
                        lang,
                        "failed_" + System.currentTimeMillis(),
                        e.getMessage(),
                        recipientId,
                        toJson(vars)
                );
            } catch (Exception logError) {
                log.error("Failed to log failed WhatsApp message", logError);
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /** Resend a failed row in place (dashboard retry). */
    public void retryExistingMessage(
            long existingDbId,
            String to,
            String templateName,
            String templateLanguage,
            Map<String, Object> variables,
            String scopeKey,
            Long recipientId
    ) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("WhatsApp is disabled (set WHATSAPP_ENABLED=true)");
        }

        final String scope = scopeKey != null ? scopeKey : properties.getScopeKey();
        String formattedPhone = WhatsAppPhoneFormatter.format(to);
        Map<String, Object> vars = variables != null ? new LinkedHashMap<>(variables) : new LinkedHashMap<>();
        String lang = templateLanguage != null ? templateLanguage : "hi";

        checkCircuitBreaker();
        if (!validateTemplate(templateName, lang, scope)) {
            throw new IllegalStateException(
                    "Template '" + templateName + "' not found or not approved (scope=" + scope + ")");
        }
        rateLimiter.enforceRateLimit(scope);

        List<Map<String, Object>> components = componentBuilder.build(vars, templateName, lang, scope, null, null);
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", formattedPhone,
                "type", "template",
                "template", Map.of(
                        "name", templateName,
                        "language", Map.of("code", lang),
                        "components", components
                )
        );

        log.info("Retrying WhatsApp message id={}: template={}, phone={}", existingDbId, templateName, formattedPhone);

        String path = "/" + properties.getApiVersion() + "/" + properties.getPhoneNumberId() + "/messages";
        JsonNode response = restClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        String messageId = extractMessageId(response);
        jdbcTemplate.update(
                """
                UPDATE whatsapp_messages
                SET message_id = ?, message_status = 'sent', template_language = ?,
                    variables = ?::jsonb, failure_reason = NULL, failed_at = NULL,
                    sent_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND org_id = ?
                """,
                messageId,
                lang,
                toJson(vars),
                existingDbId,
                scope
        );
        recordApiResult(true);
    }

    public BulkSendResult sendBulkTemplateMessages(
            String templateName,
            String templateLanguage,
            List<WhatsAppRecipient> recipients,
            BulkMessagingOptions options
    ) {
        return sendBulkTemplateMessages(templateName, templateLanguage, recipients, properties.getScopeKey(), options);
    }

    public BulkSendResult sendBulkTemplateMessages(
            String templateName,
            String templateLanguage,
            List<WhatsAppRecipient> recipients,
            String scopeKey,
            BulkMessagingOptions options
    ) {
        return bulkMessagingService.sendBulkTemplateMessages(
                templateName,
                templateLanguage,
                recipients,
                scopeKey != null ? scopeKey : properties.getScopeKey(),
                this::sendFromQueue,
                options
        );
    }

    public String uploadMedia(byte[] buffer, String mimeType, String filename) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("messaging_product", "whatsapp");
        builder.part("file", new ByteArrayResource(buffer) {
            @Override
            public String getFilename() {
                return filename;
            }
        }).contentType(MediaType.parseMediaType(mimeType));

        MultiValueMap<String, org.springframework.http.HttpEntity<?>> body = builder.build();

        String path = "/" + properties.getApiVersion() + "/" + properties.getPhoneNumberId() + "/media";
        JsonNode response = restClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response != null && response.has("id")) {
            return response.get("id").asText();
        }
        throw new IllegalStateException("Failed to get media ID from WhatsApp upload response");
    }

    public int cleanupOldQueueMessages(int daysOld) {
        return queueService.cleanupOldMessages(daysOld);
    }

    public void processQueuedMessages() {
        queueService.processQueuedMessages(this::sendFromQueue, null, null);
    }

    private void sendFromQueue(
            String phoneNumber,
            String templateName,
            String templateLanguage,
            Map<String, Object> variables,
            String orgId,
            String mediaId,
            boolean skipQueue,
            Long recipientId,
            String documentFilename
    ) throws Exception {
        sendTemplateMessage(phoneNumber, templateName, templateLanguage, variables, orgId,
                mediaId, skipQueue, recipientId, documentFilename);
    }

    private boolean validateTemplate(String templateName, String templateLanguage, String scopeKey) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM whatsapp_templates
                WHERE template_name = ? AND template_language = ? AND org_id = ? AND template_status = 'approved'
                """,
                Integer.class,
                templateName,
                templateLanguage,
                scopeKey
        );
        return count != null && count > 0;
    }

    private void checkCircuitBreaker() {
        long now = System.currentTimeMillis();
        if (consecutiveFailures >= CIRCUIT_BREAKER_THRESHOLD) {
            if (now - lastFailureTime < CIRCUIT_BREAKER_TIMEOUT_MS) {
                throw new IllegalStateException("Circuit breaker is open. Too many consecutive API failures.");
            }
            consecutiveFailures = 0;
            log.info("WhatsApp circuit breaker reset after timeout");
        }
    }

    private String extractMessageId(JsonNode response) {
        if (response != null && response.path("messages").isArray() && !response.path("messages").isEmpty()) {
            return response.path("messages").get(0).path("id").asText();
        }
        recordApiResult(false);
        throw new IllegalStateException("Failed to send message — no message id in response");
    }

    private String toJson(Map<String, Object> vars) {
        try {
            return objectMapper.writeValueAsString(vars != null ? vars : Map.of());
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void recordApiResult(boolean success) {
        recordApiResult(success, null);
    }

    private void recordApiResult(boolean success, Exception error) {
        if (success) {
            consecutiveFailures = 0;
            return;
        }
        if (error instanceof RestClientResponseException rce) {
            int status = rce.getStatusCode().value();
            if (status == 400) {
                return;
            }
            if (status == 401 || status == 403 || status == 429 || status >= 500) {
                consecutiveFailures++;
                lastFailureTime = System.currentTimeMillis();
            }
        } else if (error != null) {
            consecutiveFailures++;
            lastFailureTime = System.currentTimeMillis();
        }
    }
}
