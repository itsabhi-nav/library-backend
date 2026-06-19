package com.library.library_backend.whatsapp.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record WhatsAppDashboardMessage(
        Long id,
        String messageId,
        String recipientPhone,
        String templateName,
        String status,
        String errorMessage,
        Instant sentAt,
        Instant deliveredAt,
        Instant readAt,
        Long memberId,
        String memberName,
        String memberMemberId,
        JsonNode variables
) {}
