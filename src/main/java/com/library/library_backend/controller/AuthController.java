package com.library.library_backend.controller;

import com.library.library_backend.config.AdminPinService;
import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.*;
import com.library.library_backend.model.User;
import com.library.library_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AdminPinService adminPinService;

    @PostMapping("/register")
    public ResponseEntity<User> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        checkAdmin(getAuthenticatedUser(authHeader));
        try {
            User user = userService.registerMember(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<User> getMe(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData tokenData = getAuthenticatedUser(authHeader);
        User user = userService.findById(tokenData.userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changeOwnPassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Admin-Pin", required = false) String adminPin) {
        TokenService.TokenData tokenData = getAuthenticatedUser(authHeader);
        if ("ADMIN".equals(tokenData.role)) {
            adminPinService.requirePin(adminPin);
        }
        try {
            userService.changeOwnPassword(tokenData.userId, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/verify-admin-pin")
    public ResponseEntity<Void> verifyAdminPin(
            @Valid @RequestBody AdminPinRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData tokenData = getAuthenticatedUser(authHeader);
        checkAdminOrLibrarian(tokenData);
        adminPinService.requirePin(request.getPin());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/members")
    public ResponseEntity<List<User>> getMembers(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        getAuthenticatedUser(authHeader); // Require authentication
        List<User> members = userService.getAllMembers();
        return ResponseEntity.ok(members);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        getAuthenticatedUser(authHeader);
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/students")
    public ResponseEntity<Page<User>> getStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "all") String status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        checkAdminOrLibrarian(getAuthenticatedUser(authHeader));
        Page<User> result = userService.searchStudents(
                search, status,
                PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/by-email")
    public ResponseEntity<User> getUserByEmail(
            @RequestParam String email,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData tokenData = getAuthenticatedUser(authHeader);
        checkAdminOrLibrarian(tokenData);
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + email));
        return ResponseEntity.ok(user);
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

    @PostMapping("/students/register")
    public ResponseEntity<StudentRegisterResponse> registerStudent(
            @Valid @RequestBody StudentRegisterRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData tokenData = getAuthenticatedUser(authHeader);
        checkAdminOrLibrarian(tokenData);
        try {
            StudentRegisterResponse response = userService.registerStudent(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/students/{id}/activate")
    public ResponseEntity<User> activateStudent(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData tokenData = getAuthenticatedUser(authHeader);
        checkAdminOrLibrarian(tokenData);
        try {
            User user = userService.setActiveStatus(id, true);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/students/{id}/deactivate")
    public ResponseEntity<User> deactivateStudent(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData tokenData = getAuthenticatedUser(authHeader);
        checkAdminOrLibrarian(tokenData);
        try {
            User user = userService.setActiveStatus(id, false);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/students/{id}/reset-password")
    public ResponseEntity<User> resetStudentPassword(
            @PathVariable Long id,
            @RequestBody PasswordResetRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData tokenData = getAuthenticatedUser(authHeader);
        checkAdmin(tokenData);
        try {
            User user = userService.resetPassword(id, request.getNewPassword());
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<User> updateStudentDetails(
            @PathVariable Long id,
            @RequestBody StudentRegisterRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData tokenData = getAuthenticatedUser(authHeader);
        checkAdminOrLibrarian(tokenData);
        try {
            User user = userService.updateStudent(id, request);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/by-memberid")
    public ResponseEntity<User> getUserByMemberId(
            @RequestParam String memberId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData tokenData = getAuthenticatedUser(authHeader);
        checkAdminOrLibrarian(tokenData);
        User user = userService.findByMemberId(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with memberId: " + memberId));
        return ResponseEntity.ok(user);
    }

    private void checkAdminOrLibrarian(TokenService.TokenData tokenData) {
        if (!"ADMIN".equals(tokenData.role) && !"LIBRARIAN".equals(tokenData.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Admin or Librarian role required.");
        }
    }

    private void checkAdmin(TokenService.TokenData tokenData) {
        if (!"ADMIN".equals(tokenData.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Admin role required.");
        }
    }
}

