package com.library.library_backend.whatsapp.dto;

public record QueueProcessResult(
        int processedCount,
        int sentCount,
        int failedCount,
        java.util.List<String> errors
) {
    public static QueueProcessResult empty() {
        return new QueueProcessResult(0, 0, 0, java.util.List.of());
    }
}
