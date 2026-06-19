package com.library.library_backend.whatsapp.dto;

public record WhatsAppDashboardStats(
        long total,
        long successful,
        long sent,
        long delivered,
        long read,
        long failed,
        long todayMessages,
        int successRate,
        long queuePending,
        long queueFailed
) {}
