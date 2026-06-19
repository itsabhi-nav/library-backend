package com.library.library_backend.controller;

import com.library.library_backend.config.AdminPinService;
import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.*;
import com.library.library_backend.model.FeeInvoice;
import com.library.library_backend.service.FeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/fees")
public class FeeController {

    @Autowired private FeeService feeService;
    @Autowired private TokenService tokenService;
    @Autowired private AdminPinService adminPinService;

    @PostMapping("/generate")
    public ResponseEntity<FeeGenerateResponse> generate(
            @RequestParam int year,
            @RequestParam int month,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Admin-Pin", required = false) String adminPin) {
        checkAdmin(getAuth(authHeader));
        requireAdminPin(adminPin);
        try {
            return ResponseEntity.ok(feeService.generateForMonth(year, month));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Page<FeeInvoice>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Admin-Pin", required = false) String adminPin) {
        checkAdminOrLibrarian(getAuth(authHeader));
        requireAdminPin(adminPin);
        Page<FeeInvoice> result = feeService.searchInvoices(
                year, month, status, search,
                PageRequest.of(page, Math.min(size, 100), Sort.by("billingYear").descending().and(Sort.by("billingMonth").descending())));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<FeeStatsResponse> stats(
            @RequestParam int year,
            @RequestParam int month,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Admin-Pin", required = false) String adminPin) {
        checkAdminOrLibrarian(getAuth(authHeader));
        requireAdminPin(adminPin);
        return ResponseEntity.ok(feeService.getStats(year, month));
    }

    @GetMapping("/my")
    public ResponseEntity<List<FeeInvoice>> myFees(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData auth = getAuth(authHeader);
        return ResponseEntity.ok(feeService.getMyInvoices(auth.userId));
    }

    @GetMapping("/my/current")
    public ResponseEntity<FeeInvoice> myCurrentFee(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData auth = getAuth(authHeader);
        FeeInvoice inv = feeService.getCurrentMonthInvoice(auth.userId);
        if (inv == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(inv);
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<FeeInvoice> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody FeePaymentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Admin-Pin", required = false) String adminPin) {
        TokenService.TokenData auth = getAuth(authHeader);
        checkAdminOrLibrarian(auth);
        requireAdminPin(adminPin);
        try {
            return ResponseEntity.ok(feeService.recordPayment(id, request, auth.userId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{id}/waive")
    public ResponseEntity<FeeInvoice> waive(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Admin-Pin", required = false) String adminPin) {
        checkAdmin(getAuth(authHeader));
        requireAdminPin(adminPin);
        try {
            return ResponseEntity.ok(feeService.waiveInvoice(id));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private void requireAdminPin(String adminPin) {
        adminPinService.requirePin(adminPin);
    }

    private TokenService.TokenData getAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization header");
        }
        TokenService.TokenData data = tokenService.validateToken(authHeader.substring(7));
        if (data == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        return data;
    }

    private void checkAdmin(TokenService.TokenData auth) {
        if (!"ADMIN".equals(auth.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin required");
    }

    private void checkAdminOrLibrarian(TokenService.TokenData auth) {
        if (!"ADMIN".equals(auth.role) && !"LIBRARIAN".equals(auth.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin or Librarian required");
        }
    }
}
