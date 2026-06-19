package com.library.library_backend.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.StudyLogEntryResponse;
import com.library.library_backend.dto.StudyLogHistoryResponse;
import com.library.library_backend.dto.StudyLogRequest;
import com.library.library_backend.service.StudyLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/study-log")
public class StudyLogController {

    @Autowired
    private StudyLogService studyLogService;

    @Autowired
    private TokenService tokenService;

    @GetMapping
    public ResponseEntity<StudyLogHistoryResponse> history(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        TokenService.TokenData auth = requireMember(authHeader);
        return ResponseEntity.ok(studyLogService.getHistory(auth.userId, year, month));
    }

    @PostMapping
    public ResponseEntity<StudyLogEntryResponse> addLog(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody StudyLogRequest request) {
        TokenService.TokenData auth = requireMember(authHeader);
        try {
            return ResponseEntity.ok(studyLogService.addLog(auth.userId, request));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private TokenService.TokenData requireMember(String authHeader) {
        TokenService.TokenData auth = getAuthenticatedUser(authHeader);
        if (!"MEMBER".equals(auth.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Members only");
        }
        return auth;
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
