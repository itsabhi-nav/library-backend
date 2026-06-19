package com.library.library_backend.service;

import com.library.library_backend.dto.ExamDefinitionResponse;
import com.library.library_backend.dto.UserExamTargetResponse;
import com.library.library_backend.model.ExamDefinition;
import com.library.library_backend.model.User;
import com.library.library_backend.model.UserExamTarget;
import com.library.library_backend.repository.ExamDefinitionRepository;
import com.library.library_backend.repository.UserExamTargetRepository;
import com.library.library_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExamTargetService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private ExamDefinitionRepository examDefinitionRepository;

    @Autowired
    private UserExamTargetRepository userExamTargetRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ExamDefinitionResponse> listExams() {
        return examDefinitionRepository.findAllByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserExamTargetResponse getMyTarget(Long userId) {
        return userExamTargetRepository.findByUserId(userId)
                .map(this::toUserTargetResponse)
                .orElse(null);
    }

    @Transactional
    public UserExamTargetResponse setMyTarget(Long userId, String examCode) {
        if (examCode == null || examCode.isBlank()) {
            throw new IllegalArgumentException("Exam code is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ExamDefinition exam = examDefinitionRepository.findByCode(examCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Unknown exam: " + examCode));

        UserExamTarget target = userExamTargetRepository.findByUserId(userId)
                .orElse(UserExamTarget.builder().user(user).build());
        target.setExamDefinition(exam);
        target = userExamTargetRepository.save(target);
        return toUserTargetResponse(target);
    }

    ExamDefinitionResponse toResponse(ExamDefinition exam) {
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        long days = ChronoUnit.DAYS.between(today, exam.getExamDate());
        if (days < 0) {
            days = 0;
        }
        return ExamDefinitionResponse.builder()
                .code(exam.getCode())
                .name(exam.getName())
                .examLabel(exam.getExamLabel())
                .examDate(exam.getExamDate().toString())
                .daysRemaining(days)
                .build();
    }

    private UserExamTargetResponse toUserTargetResponse(UserExamTarget target) {
        ExamDefinitionResponse exam = toResponse(target.getExamDefinition());
        String message = exam.getDaysRemaining() + " days left for " + exam.getExamLabel();
        return UserExamTargetResponse.builder()
                .selectedExam(exam)
                .motivationalMessage(message)
                .build();
    }
}
