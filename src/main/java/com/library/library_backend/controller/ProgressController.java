package com.library.library_backend.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.ProgressOverviewResponse;
import com.library.library_backend.service.ProgressOverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    @Autowired
    private ProgressOverviewService progressOverviewService;

    @Autowired
    private TokenService tokenService;

    @GetMapping("/overview")
    public ResponseEntity<ProgressOverviewResponse> overview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        TokenService.TokenData auth = getAuthenticatedUser(authHeader);
        if (!"MEMBER".equals(auth.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Members only");
        }
        return ResponseEntity.ok(progressOverviewService.getOverview(auth.userId, year, month));
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
