package com.library.library_backend.whatsapp.service;

import com.library.library_backend.whatsapp.dto.BulkMessagingOptions;
import com.library.library_backend.whatsapp.dto.BulkSendResult;
import com.library.library_backend.whatsapp.dto.SendMessageCallback;
import com.library.library_backend.whatsapp.dto.WhatsAppRecipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WhatsAppBulkMessagingService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppBulkMessagingService.class);

    private final WhatsAppQueueService queueService;

    public WhatsAppBulkMessagingService(WhatsAppQueueService queueService) {
        this.queueService = queueService;
    }

    public BulkSendResult sendBulkTemplateMessages(
            String templateName,
            String templateLanguage,
            List<WhatsAppRecipient> recipients,
            String orgId,
            SendMessageCallback sendMessageCallback,
            BulkMessagingOptions options
    ) {
        long start = Instant.now().toEpochMilli();
        BulkMessagingOptions opts = options != null ? options : new BulkMessagingOptions();

        if (templateName == null || templateLanguage == null || orgId == null) {
            return BulkSendResult.failure("Missing required parameters for bulk messaging", 0,
                    Instant.now().toEpochMilli() - start);
        }
        if (recipients == null || recipients.isEmpty()) {
            return BulkSendResult.failure("Recipients array is required and must not be empty", 0,
                    Instant.now().toEpochMilli() - start);
        }

        try {
            String batchId = WhatsAppQueueService.newBatchId("bulk");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("templateName", templateName);
            metadata.put("templateLanguage", templateLanguage);
            metadata.put("operation", "bulk_messaging");
            metadata.put("scheduledFor", Instant.now().toString());
            if (opts.getMediaId() != null) {
                metadata.put("mediaId", opts.getMediaId());
            }
            if (opts.getDocumentFilename() != null) {
                metadata.put("documentFilename", opts.getDocumentFilename());
            }

            queueService.addMessagesToQueue(
                    recipients, templateName, templateLanguage, orgId, batchId, metadata, 5);

            queueService.nudgeProcessor(sendMessageCallback, batchId, opts.getOnComplete());

            long duration = Instant.now().toEpochMilli() - start;
            log.info("Bulk messaging queued: orgId={}, template={}, recipients={}, batchId={}",
                    orgId, templateName, recipients.size(), batchId);

            BulkSendResult result = new BulkSendResult();
            result.setSuccess(true);
            result.setSentCount(0);
            result.setFailedCount(0);
            result.setQueuedCount(recipients.size());
            result.setDurationMs(duration);
            return result;
        } catch (Exception e) {
            log.error("Bulk messaging failed: orgId={}, template={}, error={}",
                    orgId, templateName, e.getMessage());
            return BulkSendResult.failure(e.getMessage(), recipients.size(),
                    Instant.now().toEpochMilli() - start);
        }
    }
}
