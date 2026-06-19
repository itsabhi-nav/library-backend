package com.library.library_backend.whatsapp.dto;

public record WhatsAppDashboardPagination(
        int page,
        int pageSize,
        long total,
        int totalPages
) {}
