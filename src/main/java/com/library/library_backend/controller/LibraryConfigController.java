package com.library.library_backend.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.model.LibraryConfig;
import com.library.library_backend.repository.LibraryConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/config")
public class LibraryConfigController {

    private static final Set<String> PUBLIC_CONFIG_KEYS = Set.of(
            "library_name",
            "school_affiliation",
            "developer_credit"
    );

    @Autowired
    private LibraryConfigRepository configRepository;

    @Autowired
    private TokenService tokenService;

    @GetMapping
    public ResponseEntity<List<LibraryConfig>> getAllConfigs(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        List<LibraryConfig> all = configRepository.findAll();
        TokenService.TokenData auth = tryGetAuth(authHeader);
        if (auth != null && ("ADMIN".equals(auth.role) || "LIBRARIAN".equals(auth.role))) {
            return ResponseEntity.ok(all);
        }
        return ResponseEntity.ok(all.stream()
                .filter(c -> PUBLIC_CONFIG_KEYS.contains(c.getConfigKey()))
                .toList());
    }

    @PutMapping("/{key}")
    public ResponseEntity<LibraryConfig> updateConfig(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String key,
            @RequestParam String value) {
        TokenService.TokenData authUser = getAuthenticatedUser(authHeader);
        if (!"ADMIN".equals(authUser.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required");
        }
        LibraryConfig config = configRepository.findById(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Config key not found: " + key));
        config.setConfigValue(value);
        return ResponseEntity.ok(configRepository.save(config));
    }

    private TokenService.TokenData tryGetAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return tokenService.validateToken(authHeader.substring(7));
    }

    private TokenService.TokenData getAuthenticatedUser(String authHeader) {
        TokenService.TokenData data = tryGetAuth(authHeader);
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        return data;
    }
}
