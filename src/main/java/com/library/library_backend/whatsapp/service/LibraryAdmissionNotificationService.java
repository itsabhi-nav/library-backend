package com.library.library_backend.whatsapp.service;

import com.library.library_backend.whatsapp.config.WhatsAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class LibraryAdmissionNotificationService {

    private static final Logger log = LoggerFactory.getLogger(LibraryAdmissionNotificationService.class);

    public static final String TEMPLATE_NAME = "library_admission";
    public static final String TEMPLATE_LANGUAGE = "en";

    private final WhatsAppService whatsAppService;
    private final WhatsAppProperties properties;
    private final JdbcTemplate jdbcTemplate;

    public LibraryAdmissionNotificationService(WhatsAppService whatsAppService,
                                               WhatsAppProperties properties,
                                               JdbcTemplate jdbcTemplate) {
        this.whatsAppService = whatsAppService;
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Send welcome WhatsApp once per member when consent is on (register or save details). */
    public void sendAdmissionConfirmationIfNeeded(String memberName, String phoneNumber, Long userId, Boolean whatsappConsent) {
        if (!Boolean.TRUE.equals(whatsappConsent)) {
            log.info("WhatsApp consent false — skipping admission message for userId={}", userId);
            return;
        }
        if (memberName == null || memberName.isBlank() || phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }
        if (userId != null && hasAdmissionMessageBeenSent(userId)) {
            log.info("Admission WhatsApp already sent for userId={} — skipping", userId);
            return;
        }
        sendAdmissionConfirmationAsync(memberName, phoneNumber, userId, whatsappConsent);
    }

    private boolean hasAdmissionMessageBeenSent(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM whatsapp_messages
                WHERE student_id = ? AND template_name = ?
                """,
                Integer.class,
                userId,
                TEMPLATE_NAME
        );
        return count != null && count > 0;
    }

    /** Fire-and-forget admission WhatsApp. */
    public void sendAdmissionConfirmationAsync(String memberName, String phoneNumber, Long userId, Boolean whatsappConsent) {
        if (!Boolean.TRUE.equals(whatsappConsent)) {
            log.info("WhatsApp consent false — skipping admission message for userId={}", userId);
            return;
        }
        if (!properties.isEnabled()) {
            log.warn("WhatsApp disabled (WHATSAPP_ENABLED=false) — skipping admission message for {}", phoneNumber);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                sendAdmissionConfirmation(memberName, phoneNumber, userId);
            } catch (Exception e) {
                log.warn("Library admission WhatsApp failed for userId={}: {}", userId, e.getMessage());
            }
        });
    }

    public void sendAdmissionConfirmation(String memberName, String phoneNumber, Long userId) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("WhatsApp is disabled (set WHATSAPP_ENABLED=true)");
        }
        if (memberName == null || memberName.isBlank()) {
            throw new IllegalArgumentException("Member name is required");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("1", memberName.trim());

        whatsAppService.sendTemplateMessage(
                phoneNumber,
                TEMPLATE_NAME,
                TEMPLATE_LANGUAGE,
                variables,
                null,
                null,
                false,
                userId,
                null
        );

        log.info("Queued library admission WhatsApp for member={}, userId={}", memberName, userId);
    }
}
