package com.library.library_backend.config;

import com.library.library_backend.whatsapp.config.WhatsAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProductionConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionConfigValidator.class);

    @Value("${app.token.secret:}")
    private String tokenSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.admin.pin:}")
    private String adminPin;

    private final WhatsAppProperties whatsAppProperties;

    public ProductionConfigValidator(WhatsAppProperties whatsAppProperties) {
        this.whatsAppProperties = whatsAppProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        if (tokenSecret == null || tokenSecret.isBlank()
                || tokenSecret.contains("do-not-share")
                || tokenSecret.length() < 32) {
            log.warn("SECURITY: Set a strong APP_TOKEN_SECRET (32+ chars) via environment variable before production deploy.");
        }
        if (dbPassword == null || dbPassword.isBlank()) {
            log.warn("SECURITY: DATABASE_PASSWORD is not set — database connection will fail.");
        }
        if (adminPin == null || adminPin.isBlank()) {
            log.warn("SECURITY: Set APP_ADMIN_PIN (6-digit PIN) in environment for fee and password protection.");
        }
        if (whatsAppProperties.isEnabled()) {
            if (whatsAppProperties.getAccessToken() == null || whatsAppProperties.getAccessToken().isBlank()) {
                log.warn("WHATSAPP: WHATSAPP_ENABLED=true but WHATSAPP_ACCESS_TOKEN is not set.");
            }
            if (whatsAppProperties.getPhoneNumberId() == null || whatsAppProperties.getPhoneNumberId().isBlank()) {
                log.warn("WHATSAPP: WHATSAPP_ENABLED=true but WHATSAPP_PHONE_NUMBER_ID is not set.");
            }
        }
    }
}
