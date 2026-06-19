package com.library.library_backend.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.PersonalAnalyticsResponse;
import com.library.library_backend.service.UserMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private UserMetricsService userMetricsService;

    @Autowired
    private TokenService tokenService;

    @GetMapping("/me")
    public ResponseEntity<PersonalAnalyticsResponse> myAnalytics(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        TokenService.TokenData auth = getAuthenticatedUser(authHeader);
        if (!"MEMBER".equals(auth.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Members only");
        }
        return ResponseEntity.ok(userMetricsService.getPersonalAnalytics(auth.userId, year, month));
    }

    private TokenService.TokenData getAuthenticatedUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization header");
        }
        TokenService.TokenData data = tokenService.validateToken(authHeader.substring(7));
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        return data;
    }
}
