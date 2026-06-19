package com.library.library_backend.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.AdminDashboardResponse;
import com.library.library_backend.dto.FeeStatsResponse;
import com.library.library_backend.model.LibraryConfig;
import com.library.library_backend.model.Seat;
import com.library.library_backend.repository.AttendanceRepository;
import com.library.library_backend.repository.LibraryConfigRepository;
import com.library.library_backend.repository.SeatRepository;
import com.library.library_backend.service.FeeService;
import com.library.library_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private UserService userService;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private FeeService feeService;
    @Autowired private LibraryConfigRepository configRepository;
    @Autowired private TokenService tokenService;

    @GetMapping("/admin")
    public ResponseEntity<AdminDashboardResponse> adminDashboard(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData auth = getAuth(authHeader);
        if (!"ADMIN".equals(auth.role) && !"LIBRARIAN".equals(auth.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        LocalDate now = LocalDate.now();
        FeeStatsResponse fees = feeService.getStats(now.getYear(), now.getMonthValue());
        List<Seat> seats = seatRepository.findAll();
        long available = seats.stream().filter(s -> s.getStatus() == Seat.SeatStatus.AVAILABLE).count();

        String libraryName = configRepository.findById("library_name")
                .map(LibraryConfig::getConfigValue).orElse("Library");

        return ResponseEntity.ok(new AdminDashboardResponse(
                userService.countAllMembers(),
                userService.countActiveMembers(),
                attendanceRepository.countByCheckOutTimeIsNull(),
                seats.size(),
                available,
                fees.getTotalOutstanding(),
                fees.getTotalCollected(),
                fees.getOverdueCount(),
                libraryName
        ));
    }

    private TokenService.TokenData getAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization");
        }
        TokenService.TokenData data = tokenService.validateToken(authHeader.substring(7));
        if (data == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        return data;
    }
}
