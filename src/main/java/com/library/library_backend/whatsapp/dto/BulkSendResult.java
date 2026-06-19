package com.library.library_backend.whatsapp.dto;

import java.util.ArrayList;
import java.util.List;

public class BulkSendResult {

    private boolean success;
    private int sentCount;
    private int failedCount;
    private List<String> errors = new ArrayList<>();
    private long durationMs;
    private int queuedCount;

    public static BulkSendResult failure(String error, int recipientCount, long durationMs) {
        BulkSendResult r = new BulkSendResult();
        r.success = false;
        r.failedCount = recipientCount;
        r.durationMs = durationMs;
        r.errors.add(error);
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getSentCount() {
        return sentCount;
    }

    public void setSentCount(int sentCount) {
        this.sentCount = sentCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public int getQueuedCount() {
        return queuedCount;
    }

    public void setQueuedCount(int queuedCount) {
        this.queuedCount = queuedCount;
    }
}
