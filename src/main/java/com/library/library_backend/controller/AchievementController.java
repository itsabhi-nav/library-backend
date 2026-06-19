package com.library.library_backend.controller;

import com.library.library_backend.config.TokenService;
import com.library.library_backend.dto.AchievementProgressResponse;
import com.library.library_backend.dto.UserAchievementsResponse;
import com.library.library_backend.model.AchievementDefinition;
import com.library.library_backend.repository.AchievementDefinitionRepository;
import com.library.library_backend.service.AchievementEvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    @Autowired
    private AchievementDefinitionRepository definitionRepository;

    @Autowired
    private AchievementEvaluationService achievementEvaluationService;

    @Autowired
    private TokenService tokenService;

    @GetMapping("/definitions")
    public ResponseEntity<List<AchievementProgressResponse>> definitions(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        getAuthenticatedUser(authHeader);
        List<AchievementProgressResponse> defs = definitionRepository.findAllByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toDefinitionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(defs);
    }

    @GetMapping("/me")
    public ResponseEntity<UserAchievementsResponse> myAchievements(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenData auth = getAuthenticatedUser(authHeader);
        if (!"MEMBER".equals(auth.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Members only");
        }
        return ResponseEntity.ok(achievementEvaluationService.getUserAchievements(auth.userId));
    }

    private AchievementProgressResponse toDefinitionResponse(AchievementDefinition def) {
        return AchievementProgressResponse.builder()
                .id(def.getId())
                .code(def.getCode())
                .category(def.getCategory())
                .title(def.getTitle())
                .description(def.getDescription())
                .iconKey(def.getIconKey())
                .thresholdValue(def.getThresholdValue())
                .thresholdUnit(def.getThresholdUnit())
                .sortOrder(def.getSortOrder())
                .earned(false)
                .progressPercent(0)
                .currentValue(0)
                .build();
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
