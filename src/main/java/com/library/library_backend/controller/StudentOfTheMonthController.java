package com.library.library_backend.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.StudentOfTheMonthResponse;
import com.library.library_backend.service.StudentOfTheMonthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/student-of-the-month")
public class StudentOfTheMonthController {

    @Autowired
    private StudentOfTheMonthService studentOfTheMonthService;

    @Autowired
    private TokenService tokenService;

    @GetMapping
    public ResponseEntity<StudentOfTheMonthResponse> getStudentOfTheMonth(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        getAuthenticatedUser(authHeader);
        return ResponseEntity.ok(studentOfTheMonthService.getStudentOfTheMonth(year, month));
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
