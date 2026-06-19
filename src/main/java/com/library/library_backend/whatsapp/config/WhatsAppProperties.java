package com.library.library_backend.whatsapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.whatsapp")
public class WhatsAppProperties {

    /** Fixed DB scope key — library is single-tenant (no org concept). */
    public static final String SCOPE_KEY = "library";

    private boolean enabled = false;
    private String accessToken = "";
    private String phoneNumberId = "";
    private String webhookVerifyToken = "";
    private String apiVersion = "v21.0";
    private String baseUrl = "https://graph.facebook.com";
    private int rateLimitPerSecond = 80;
    private int rateLimitPerDay = 10000;
    private int queueRetentionDays = 10;
    /** Comma-separated admin phones for monthly collection / report templates. */
    private String monthlyCollectionRecipients = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getWebhookVerifyToken() {
        return webhookVerifyToken;
    }

    public void setWebhookVerifyToken(String webhookVerifyToken) {
        this.webhookVerifyToken = webhookVerifyToken;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getRateLimitPerSecond() {
        return rateLimitPerSecond;
    }

    public void setRateLimitPerSecond(int rateLimitPerSecond) {
        this.rateLimitPerSecond = rateLimitPerSecond;
    }

    public int getRateLimitPerDay() {
        return rateLimitPerDay;
    }

    public void setRateLimitPerDay(int rateLimitPerDay) {
        this.rateLimitPerDay = rateLimitPerDay;
    }

    public int getQueueRetentionDays() {
        return queueRetentionDays;
    }

    public void setQueueRetentionDays(int queueRetentionDays) {
        this.queueRetentionDays = queueRetentionDays;
    }

    public String getMonthlyCollectionRecipients() {
        return monthlyCollectionRecipients;
    }

    public void setMonthlyCollectionRecipients(String monthlyCollectionRecipients) {
        this.monthlyCollectionRecipients = monthlyCollectionRecipients;
    }

    /** Always {@link #SCOPE_KEY} — stored in org_id columns for DB compatibility. */
    public String getScopeKey() {
        return SCOPE_KEY;
    }

    public List<String> getMonthlyCollectionRecipientList() {
        if (monthlyCollectionRecipients == null || monthlyCollectionRecipients.isBlank()) {
            return List.of();
        }
        return Arrays.stream(monthlyCollectionRecipients.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
