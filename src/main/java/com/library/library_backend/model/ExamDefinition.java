package com.library.library_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "exam_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "exam_label", nullable = false, length = 128)
    private String examLabel;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
