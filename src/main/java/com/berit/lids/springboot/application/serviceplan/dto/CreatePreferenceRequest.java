package com.berit.lids.springboot.application.serviceplan.dto;

import com.berit.lids.springboot.application.serviceplan.domain.enums.PreferenceType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePreferenceRequest {

    @NotNull
    private LocalDate date;

    @NotNull
    private PreferenceType preferenceType;

    private String note;
}
