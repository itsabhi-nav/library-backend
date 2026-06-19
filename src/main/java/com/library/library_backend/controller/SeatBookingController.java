package com.library.library_backend.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.*;
import com.library.library_backend.model.Booking;
import com.library.library_backend.model.Seat;
import com.library.library_backend.model.Shift;
import com.library.library_backend.service.SeatBookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SeatBookingController {

    @Autowired
    private SeatBookingService bookingService;

    @Autowired
    private TokenService tokenService;

    @GetMapping("/seats")
    public ResponseEntity<List<Seat>> getSeats() {
        return ResponseEntity.ok(bookingService.getAllSeats());
    }

    @GetMapping("/seats/assignable")
    public ResponseEntity<List<Seat>> getAssignableSeats(
            @RequestParam(required = false) Long excludeUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        getAuthenticatedUser(authHeader);
        return ResponseEntity.ok(bookingService.getAssignableSeats(excludeUserId));
    }

    @PostMapping("/seats")
    public ResponseEntity<Seat> addSeat(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody Seat seat) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.addSeat(seat));
    }

    @PutMapping("/seats/{id}/status")
    public ResponseEntity<Seat> updateSeatStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestParam Seat.SeatStatus status) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role) && !"LIBRARIAN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authorized personnel only");
        }
        try {
            return ResponseEntity.ok(bookingService.updateSeatStatus(id, status));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/shifts")
    public ResponseEntity<List<Shift>> getShifts() {
        return ResponseEntity.ok(bookingService.getAllShifts());
    }

    @PostMapping("/shifts")
    public ResponseEntity<Shift> addShift(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody Shift shift) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.addShift(shift));
    }

    @GetMapping("/bookings/date/{date}")
    public ResponseEntity<List<Booking>> getBookingsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getBookingsByDate(date));
    }

    @PostMapping("/bookings")
    public ResponseEntity<Booking> createBooking(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody BookingRequest request) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        try {
            Booking booking = bookingService.createBooking(authUser.userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Booking> cancelBooking(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        try {
            Booking booking = bookingService.cancelBooking(id, authUser.userId);
            return ResponseEntity.ok(booking);
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/bookings/my")
    public ResponseEntity<List<Booking>> getMyBookings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        return ResponseEntity.ok(bookingService.getUserBookings(authUser.userId));
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

    @PutMapping("/shifts/{id}")
    public ResponseEntity<Shift> updateShift(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody ShiftRequest request) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required");
        }
        try {
            return ResponseEntity.ok(bookingService.updateShift(id, request));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/shifts/{id}")
    public ResponseEntity<Void> deleteShift(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required");
        }
        try {
            bookingService.deleteShift(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/seats/capacity")
    public ResponseEntity<Void> setSeatCapacity(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam int capacity) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required");
        }
        try {
            bookingService.bulkSetCapacity(capacity);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
