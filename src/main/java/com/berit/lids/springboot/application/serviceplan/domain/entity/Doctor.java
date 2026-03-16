package com.berit.lids.springboot.application.serviceplan.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "doctor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "extra_shifts", nullable = false)
    @Builder.Default
    private int extraShifts = 0;

    @Column(name = "color", nullable = false)
    @Builder.Default
    private String color = "#667eea";
}
