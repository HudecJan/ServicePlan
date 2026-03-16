package com.berit.lids.springboot.application.serviceplan.dto;

import com.berit.lids.springboot.application.serviceplan.domain.enums.PreferenceType;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftPreferenceDto {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private LocalDate date;
    private PreferenceType preferenceType;
    private String note;
}
