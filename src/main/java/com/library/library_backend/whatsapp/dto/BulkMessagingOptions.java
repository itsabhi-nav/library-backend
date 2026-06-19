package com.library.library_backend.whatsapp.dto;

import java.util.Map;
import java.util.function.Consumer;

public class BulkMessagingOptions {

    private int batchSize = 50;
    private String mediaId;
    private String documentFilename;
    private Consumer<BulkProgress> progressCallback;
    private Consumer<BatchCompletionResult> onComplete;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getDocumentFilename() {
        return documentFilename;
    }

    public void setDocumentFilename(String documentFilename) {
        this.documentFilename = documentFilename;
    }

    public Consumer<BulkProgress> getProgressCallback() {
        return progressCallback;
    }

    public void setProgressCallback(Consumer<BulkProgress> progressCallback) {
        this.progressCallback = progressCallback;
    }

    public Consumer<BatchCompletionResult> getOnComplete() {
        return onComplete;
    }

    public void setOnComplete(Consumer<BatchCompletionResult> onComplete) {
        this.onComplete = onComplete;
    }

    public record BulkProgress(int completed, int total, int percentage) {}

    public record BatchCompletionResult(int sentCount, int failedCount) {}
}
