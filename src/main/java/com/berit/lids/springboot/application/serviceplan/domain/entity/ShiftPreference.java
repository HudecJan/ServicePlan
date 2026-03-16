package com.berit.lids.springboot.application.serviceplan.domain.entity;

import com.berit.lids.springboot.application.serviceplan.domain.enums.PreferenceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "shift_preference",
        uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_type", nullable = false)
    private PreferenceType preferenceType;

    @Column(name = "note")
    private String note;
}
