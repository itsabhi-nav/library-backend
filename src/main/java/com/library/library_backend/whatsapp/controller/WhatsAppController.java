package com.library.library_backend.whatsapp.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.whatsapp.config.WhatsAppProperties;
import com.library.library_backend.whatsapp.dto.WhatsAppDashboardResponse;
import com.library.library_backend.whatsapp.service.LibraryAdmissionNotificationService;
import com.library.library_backend.whatsapp.service.WhatsAppDashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final LibraryAdmissionNotificationService admissionService;
    private final WhatsAppDashboardService dashboardService;
    private final WhatsAppProperties properties;
    private final TokenService tokenService;

    public WhatsAppController(LibraryAdmissionNotificationService admissionService,
                              WhatsAppDashboardService dashboardService,
                              WhatsAppProperties properties,
                              TokenService tokenService) {
        this.admissionService = admissionService;
        this.dashboardService = dashboardService;
        this.properties = properties;
        this.tokenService = tokenService;
    }

    /** Paginated message log + aggregate stats — admin only. */
    @GetMapping("/messages")
    public ResponseEntity<WhatsAppDashboardResponse> getMessages(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String templateName) {
        checkAdmin(authHeader);
        return ResponseEntity.ok(dashboardService.getDashboardData(page, pageSize, search, status, templateName));
    }

    /** Retry a failed message — admin only. */
    @PostMapping("/messages/{messageId}/retry")
    public ResponseEntity<Map<String, String>> retryMessage(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable long messageId) {
        checkAdmin(authHeader);
        if (!properties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "WhatsApp is disabled (set WHATSAPP_ENABLED=true)");
        }
        try {
            dashboardService.retryFailedMessage(messageId);
            return ResponseEntity.ok(Map.of("message", "Message retry initiated successfully"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** Manual test send — admin/librarian only. */
    @PostMapping("/test-admission")
    public ResponseEntity<Map<String, String>> testAdmission(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody TestAdmissionRequest body) {
        checkAdminOrLibrarian(authHeader);
        if (!properties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "WhatsApp is disabled (set WHATSAPP_ENABLED=true)");
        }
        if (body.phoneNumber() == null || body.phoneNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phoneNumber is required");
        }
        String name = body.memberName() != null && !body.memberName().isBlank()
                ? body.memberName() : "Test Member";

        admissionService.sendAdmissionConfirmation(name, body.phoneNumber(), body.userId());

        return ResponseEntity.ok(Map.of(
                "message", "Admission WhatsApp queued",
                "template", LibraryAdmissionNotificationService.TEMPLATE_NAME
        ));
    }

    public record TestAdmissionRequest(String phoneNumber, String memberName, Long userId) {}

    private void checkAdmin(String authHeader) {
        TokenService.TokenData data = requireToken(authHeader);
        if (!"ADMIN".equals(data.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
    }

    private void checkAdminOrLibrarian(String authHeader) {
        TokenService.TokenData data = requireToken(authHeader);
        if (!"ADMIN".equals(data.role) && !"LIBRARIAN".equals(data.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin or librarian only");
        }
    }

    private TokenService.TokenData requireToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        TokenService.TokenData data = tokenService.validateToken(authHeader.substring(7));
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        return data;
    }
}
