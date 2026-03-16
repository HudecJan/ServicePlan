package com.berit.lids.springboot.application.serviceplan.dto;

import com.berit.lids.springboot.application.serviceplan.domain.enums.ScheduleStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDto {
    private Long id;
    private int year;
    private int month;
    private ScheduleStatus status;
    private LocalDateTime generatedAt;
    private List<ShiftAssignmentDto> assignments;
}
