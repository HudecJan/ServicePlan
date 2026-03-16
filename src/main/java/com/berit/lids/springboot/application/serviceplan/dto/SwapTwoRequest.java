package com.berit.lids.springboot.application.serviceplan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SwapTwoRequest {

    @NotNull
    private Long assignmentId1;

    @NotNull
    private Long assignmentId2;
}
