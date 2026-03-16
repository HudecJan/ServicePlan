package com.berit.lids.springboot.application.serviceplan.dto;

import com.berit.lids.springboot.application.serviceplan.domain.enums.ShiftType;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftAssignmentDto {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private LocalDate date;
    private ShiftType shiftType;
    private String doctorColor;
    private boolean forced;
    private String warning;
}
