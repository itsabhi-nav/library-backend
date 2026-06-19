package com.library.library_backend.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.MonthlyGoalRequest;
import com.library.library_backend.dto.MonthlyGoalResponse;
import com.library.library_backend.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    @Autowired
    private GoalService goalService;

    @Autowired
    private TokenService tokenService;

    @GetMapping("/me")
    public ResponseEntity<MonthlyGoalResponse> myGoal(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        TokenService.TokenData auth = getAuthenticatedUser(authHeader);
        if (!"MEMBER".equals(auth.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Members only");
        }
        return ResponseEntity.ok(goalService.getGoal(auth.userId, year, month));
    }

    @PutMapping("/me")
    public ResponseEntity<MonthlyGoalResponse> setGoal(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody MonthlyGoalRequest request) {
        TokenService.TokenData auth = getAuthenticatedUser(authHeader);
        if (!"MEMBER".equals(auth.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Members only");
        }
        return ResponseEntity.ok(goalService.setGoal(auth.userId, request));
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
