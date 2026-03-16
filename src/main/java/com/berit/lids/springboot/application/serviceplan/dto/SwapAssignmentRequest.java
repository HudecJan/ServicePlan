package com.berit.lids.springboot.application.serviceplan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SwapAssignmentRequest {

    @NotNull
    private Long newDoctorId;
}
