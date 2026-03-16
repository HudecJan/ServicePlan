package com.berit.lids.springboot.application.serviceplan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateScheduleRequest {

    @NotNull
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    // date string "2026-03-05" -> doctorId who wins the conflict
    private Map<String, Long> overrides;
}
