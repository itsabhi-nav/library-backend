package com.library.library_backend.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.AttendanceStatusResponse;
import com.library.library_backend.dto.LeaderboardResponse;
import com.library.library_backend.dto.MonthlyAttendanceStatsResponse;
import com.library.library_backend.model.Attendance;
import com.library.library_backend.service.AttendanceService;
import com.library.library_backend.service.AttendanceStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceStatsService attendanceStatsService;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/check-in")
    public ResponseEntity<Attendance> checkIn(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam String memberId) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role) && !"LIBRARIAN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only staff can log attendance");
        }
        try {
            return ResponseEntity.ok(attendanceService.checkIn(memberId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/check-out")
    public ResponseEntity<Attendance> checkOut(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam String memberId) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role) && !"LIBRARIAN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only staff can log attendance");
        }
        try {
            return ResponseEntity.ok(attendanceService.checkOut(memberId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/punch-in")
    public ResponseEntity<AttendanceStatusResponse> punchInSelf(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData auth = getAuthenticatedUser(authHeader);
        try {
            return ResponseEntity.ok(attendanceService.punchInSelf(auth.userId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/punch-out")
    public ResponseEntity<AttendanceStatusResponse> punchOutSelf(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData auth = getAuthenticatedUser(authHeader);
        try {
            return ResponseEntity.ok(attendanceService.punchOutSelf(auth.userId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/me/status")
    public ResponseEntity<AttendanceStatusResponse> myStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData auth = getAuthenticatedUser(authHeader);
        return ResponseEntity.ok(attendanceService.getMyStatus(auth.userId));
    }

    @GetMapping("/me/monthly-stats")
    public ResponseEntity<MonthlyAttendanceStatsResponse> myMonthlyStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        TokenService.TokenData auth = getAuthenticatedUser(authHeader);
        return ResponseEntity.ok(attendanceStatsService.getMonthlyStats(auth.userId, year, month));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<LeaderboardResponse> leaderboard(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        TokenService.TokenData auth = getAuthenticatedUser(authHeader);
        return ResponseEntity.ok(attendanceStatsService.getLeaderboard(year, month, auth.userId));
    }

    @GetMapping("/occupied-seats")
    public ResponseEntity<List<Long>> occupiedSeats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        getAuthenticatedUser(authHeader);
        return ResponseEntity.ok(attendanceService.getSeatIdsOccupiedByPunchIn());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Attendance>> getActiveSessions(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role) && !"LIBRARIAN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return ResponseEntity.ok(attendanceService.getActiveSessions());
    }

    private TokenService.TokenData getAuthenticatedUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization header");
        }
        String token = authHeader.substring(7);
        TokenService.TokenData data = tokenService.validateToken(token);
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        return data;
    }
}
