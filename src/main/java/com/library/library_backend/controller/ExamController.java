package com.library.library_backend.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.ExamDefinitionResponse;
import com.library.library_backend.dto.SetExamTargetRequest;
import com.library.library_backend.dto.UserExamTargetResponse;
import com.library.library_backend.service.ExamTargetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamTargetService examTargetService;

    @Autowired
    private TokenService tokenService;

    @GetMapping
    public ResponseEntity<List<ExamDefinitionResponse>> listExams(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        getAuthenticatedUser(authHeader);
        return ResponseEntity.ok(examTargetService.listExams());
    }

    @GetMapping("/me")
    public ResponseEntity<UserExamTargetResponse> myExam(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData auth = requireMember(authHeader);
        return ResponseEntity.ok(examTargetService.getMyTarget(auth.userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserExamTargetResponse> setMyExam(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SetExamTargetRequest request) {
        TokenService.TokenData auth = requireMember(authHeader);
        try {
            return ResponseEntity.ok(examTargetService.setMyTarget(auth.userId, request.getExamCode()));
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
