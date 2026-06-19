package com.library.library_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminPinService {

    @Value("${app.admin.pin:}")
    private String configuredPin;

    public void requirePin(String pin) {
        if (configuredPin == null || configuredPin.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Admin PIN is not configured on the server");
        }
        if (pin == null || pin.isBlank() || !configuredPin.equals(pin.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid admin PIN");
        }
    }
}
