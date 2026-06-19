package com.library.library_backend.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.*;
import com.library.library_backend.model.MembershipPlan;
import com.library.library_backend.model.Subscription;
import com.library.library_backend.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private TokenService tokenService;

    @GetMapping("/plans")
    public ResponseEntity<List<MembershipPlan>> getPlans() {
        return ResponseEntity.ok(subscriptionService.getAllPlans());
    }

    @PostMapping("/plans")
    public ResponseEntity<MembershipPlan> createPlan(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody PlanRequest request) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.createPlan(request));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<MembershipPlan> updatePlan(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody PlanRequest request) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required");
        }
        return ResponseEntity.ok(subscriptionService.updatePlan(id, request));
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<Void> deletePlan(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required");
        }
        subscriptionService.deactivatePlan(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/plans/stats")
    public ResponseEntity<List<PlanStatsResponse>> getPlanStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role) && !"LIBRARIAN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin or Librarian permission required");
        }
        return ResponseEntity.ok(subscriptionService.getPlanStats());
    }

    @GetMapping("/plans/all")
    public ResponseEntity<List<MembershipPlan>> getAllPlansAdmin(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role) && !"LIBRARIAN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin or Librarian permission required");
        }
        return ResponseEntity.ok(subscriptionService.getAllPlansAdmin());
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<Subscription> createSubscription(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody SubscriptionRequest request) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        // Only Admin or Librarian can process manual purchases, or the user themselves (if online payment enabled).
        // Let's allow Admin, Librarian, or the matching User.
        if (!"ADMIN".equals(authUser.role) && !"LIBRARIAN".equals(authUser.role) && !authUser.userId.equals(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized action");
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.createSubscription(request));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/subscriptions/active")
    public ResponseEntity<Subscription> getActiveSubscription(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        Subscription subscription = subscriptionService.getActiveSubscription(authUser.userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active subscription found"));
        return ResponseEntity.ok(subscription);
    }

    @GetMapping("/subscriptions/user/{userId}")
    public ResponseEntity<List<Subscription>> getUserSubscriptions(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userId) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role) && !"LIBRARIAN".equals(authUser.role) && !authUser.userId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized action");
        }
        return ResponseEntity.ok(subscriptionService.getUserSubscriptions(userId));
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
