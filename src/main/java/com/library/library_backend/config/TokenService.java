package com.library.library_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class TokenService {

    private final String secretKey;

    public TokenService(@Value("${app.token.secret:bra-library-fallback-secret-key-2026}") String configuredSecret) {
        // Use the fixed secret from application.properties so tokens survive server restarts
        this.secretKey = configuredSecret;
    }

    public String generateToken(Long userId, String memberId, String role) {
        long expiry = System.currentTimeMillis() + (86400000L * 7); // 7 days validity
        String payload = userId + ":" + memberId + ":" + role + ":" + expiry;
        String signature = calculateHmac(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (payload + "::" + signature).getBytes(StandardCharsets.UTF_8)
        );
    }

    public TokenData validateToken(String tokenStr) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(tokenStr);
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] parts = decoded.split("::");
            if (parts.length != 2) return null;

            String payload = parts[0];
            String signature = parts[1];

            // Verify signature
            String expectedSignature = calculateHmac(payload);
            if (!MessageDigest.isEqual(
                    signature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8)
            )) {
                return null;
            }

            // Parse payload
            String[] data = payload.split(":");
            if (data.length != 4) return null;

            Long userId = Long.parseLong(data[0]);
            String memberId = data[1];
            String role = data[2];
            long expiry = Long.parseLong(data[3]);

            if (System.currentTimeMillis() > expiry) {
                return null; // Expired
            }

            return new TokenData(userId, memberId, role);
        } catch (Exception e) {
            return null;
        }
    }

    private String calculateHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC", e);
        }
    }

    public static class TokenData {
        public final Long userId;
        public final String memberId;
        public final String role;

        public TokenData(Long userId, String memberId, String role) {
            this.userId = userId;
            this.memberId = memberId;
            this.role = role;
        }
    }
}
