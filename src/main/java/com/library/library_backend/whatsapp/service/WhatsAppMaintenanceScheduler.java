package com.library.library_backend.whatsapp.service;

import com.library.library_backend.whatsapp.config.WhatsAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMaintenanceScheduler.class);

    private final WhatsAppProperties properties;
    private final WhatsAppQueueService queueService;
    private final WhatsAppService whatsAppService;

    public WhatsAppMaintenanceScheduler(WhatsAppProperties properties,
                                        WhatsAppQueueService queueService,
                                        WhatsAppService whatsAppService) {
        this.properties = properties;
        this.queueService = queueService;
        this.whatsAppService = whatsAppService;
    }

    /** Daily queue processing — 12:03 AM IST */
    @Scheduled(cron = "0 3 0 * * *", zone = "Asia/Kolkata")
    public void processQueueJob() {
        if (!properties.isEnabled()) {
            return;
        }
        log.info("Starting daily WhatsApp queue processing job at 12:03 AM IST");
        try {
            queueService.resetFailedMessagesForRetry();
            whatsAppService.processQueuedMessages();
        } catch (Exception e) {
            log.error("Daily WhatsApp queue processing failed: {}", e.getMessage(), e);
        }
    }

    /** Queue retention cleanup — 12:10 AM IST */
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Kolkata")
    public void cleanupQueueJob() {
        if (!properties.isEnabled()) {
            return;
        }
        log.info("Starting daily WhatsApp queue cleanup job at 12:10 AM IST");
        try {
            int deleted = whatsAppService.cleanupOldQueueMessages(properties.getQueueRetentionDays());
            log.info("WhatsApp queue cleanup completed: deleted={} rows", deleted);
        } catch (Exception e) {
            log.error("Daily WhatsApp queue cleanup failed: {}", e.getMessage(), e);
        }
    }
}
