package com.library.library_backend.whatsapp.dto;

import java.util.Map;

@FunctionalInterface
public interface SendMessageCallback {

    void send(
            String phoneNumber,
            String templateName,
            String templateLanguage,
            Map<String, Object> variables,
            String orgId,
            String mediaId,
            boolean skipQueue,
            Long recipientId,
            String documentFilename
    ) throws Exception;
}
