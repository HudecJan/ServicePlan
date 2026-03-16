package com.berit.lids.springboot.application.serviceplan.domain.entity;

import com.berit.lids.springboot.application.serviceplan.domain.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "monthly_schedule",
        uniqueConstraints = @UniqueConstraint(columnNames = {"year", "month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "\"year\"", nullable = false)
    private int year;

    @Column(name = "\"month\"", nullable = false)
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.DRAFT;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShiftAssignment> assignments = new ArrayList<>();
}
