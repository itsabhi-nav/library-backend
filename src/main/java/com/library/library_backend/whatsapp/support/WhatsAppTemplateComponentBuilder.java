package com.library.library_backend.whatsapp.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WhatsAppTemplateComponentBuilder {

    private final JdbcTemplate jdbcTemplate;

    public WhatsAppTemplateComponentBuilder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> build(
            Map<String, Object> variables,
            String templateName,
            String templateLanguage,
            String orgId,
            String mediaId,
            String documentFilename
    ) {
        List<Map<String, Object>> components = new ArrayList<>();
        Map<String, Object> safeVars = variables != null ? variables : Map.of();

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    """
                    SELECT header_type, header_content, footer_text
                    FROM whatsapp_templates
                    WHERE template_name = ? AND template_language = ? AND org_id = ?
                    """,
                    templateName,
                    templateLanguage != null ? templateLanguage : "en",
                    orgId
            );

            if (!rows.isEmpty()) {
                Map<String, Object> template = rows.get(0);
                String headerType = (String) template.get("header_type");
                String headerContent = (String) template.get("header_content");

                if (headerType != null) {
                    Map<String, Object> header = buildHeader(headerType, headerContent, mediaId, documentFilename);
                    if (header != null) {
                        components.add(header);
                    }
                }
                // Static footer is defined in Meta template — do not send footer component.
            }
        } catch (Exception ignored) {
            // Template metadata optional for send attempt
        }

        if (!safeVars.isEmpty()) {
            List<Map<String, Object>> bodyParams = new ArrayList<>();
            safeVars.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> bodyParams.add(Map.of("type", "text", "text", String.valueOf(e.getValue()))));
            components.add(Map.of("type", "body", "parameters", bodyParams));
        }

        return components;
    }

    private Map<String, Object> buildHeader(
            String headerType,
            String headerContent,
            String mediaId,
            String documentFilename
    ) {
        if ("IMAGE".equals(headerType) && mediaId != null) {
            return Map.of(
                    "type", "header",
                    "parameters", List.of(Map.of(
                            "type", "image",
                            "image", Map.of("id", mediaId)
                    ))
            );
        }
        if ("DOCUMENT".equals(headerType) && mediaId != null) {
            String safeName = documentFilename != null && !documentFilename.isBlank()
                    ? sanitizeDocumentFilename(documentFilename)
                    : "document.pdf";
            return Map.of(
                    "type", "header",
                    "parameters", List.of(Map.of(
                            "type", "document",
                            "document", Map.of("id", mediaId, "filename", safeName)
                    ))
            );
        }
        if (mediaId != null) {
            return Map.of(
                    "type", "header",
                    "parameters", List.of(Map.of(
                            "type", "image",
                            "image", Map.of("id", mediaId)
                    ))
            );
        }
        if ("IMAGE".equals(headerType) && headerContent != null && !headerContent.isBlank()) {
            return Map.of(
                    "type", "header",
                    "parameters", List.of(Map.of(
                            "type", "image",
                            "image", Map.of("link", headerContent)
                    ))
            );
        }
        if ("TEXT".equals(headerType) && headerContent != null && headerContent.contains("{{")) {
            return Map.of(
                    "type", "header",
                    "parameters", List.of(Map.of(
                            "type", "text",
                            "text", headerContent
                    ))
            );
        }
        return null;
    }

    static String sanitizeDocumentFilename(String name) {
        String n = name.trim().replaceAll("[/\\\\?%*:|\"<>]", "_").replaceAll("\\s+", "_");
        if (n.isEmpty()) {
            return "document.pdf";
        }
        if (n.length() > 200) {
            n = n.substring(0, 200);
        }
        if (!n.toLowerCase().endsWith(".pdf")) {
            n = n + ".pdf";
        }
        return n;
    }
}
