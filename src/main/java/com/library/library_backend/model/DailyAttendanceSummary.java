package com.library.library_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "daily_attendance_summary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyAttendanceSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "total_minutes", nullable = false)
    @Builder.Default
    private Integer totalMinutes = 0;

    @Column(name = "last_punch_out_time")
    private OffsetDateTime lastPunchOutTime;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
