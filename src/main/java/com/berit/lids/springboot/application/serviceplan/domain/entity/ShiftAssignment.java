package com.berit.lids.springboot.application.serviceplan.domain.entity;

import com.berit.lids.springboot.application.serviceplan.domain.enums.ShiftType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "shift_assignment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "date", "shift_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private MonthlySchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    private ShiftType shiftType;

    @Column(name = "forced", nullable = false)
    @Builder.Default
    private boolean forced = false;

    @Column(name = "warning")
    private String warning;
}
