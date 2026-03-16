package com.berit.lids.springboot.application.serviceplan.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private boolean active;
    private int extraShifts;
    private String color;
}
