package com.library.library_backend.whatsapp.dto;

import java.util.List;

public record WhatsAppDashboardResponse(
        List<WhatsAppDashboardMessage> messages,
        WhatsAppDashboardStats stats,
        WhatsAppDashboardPagination pagination
) {}
