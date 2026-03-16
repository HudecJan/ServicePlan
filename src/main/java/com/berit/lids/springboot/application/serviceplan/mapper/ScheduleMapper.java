package com.berit.lids.springboot.application.serviceplan.mapper;

import com.berit.lids.springboot.application.serviceplan.domain.entity.MonthlySchedule;
import com.berit.lids.springboot.application.serviceplan.domain.entity.ShiftAssignment;
import com.berit.lids.springboot.application.serviceplan.dto.ScheduleDto;
import com.berit.lids.springboot.application.serviceplan.dto.ShiftAssignmentDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScheduleMapper {

    public ScheduleDto toDto(MonthlySchedule schedule) {
        List<ShiftAssignmentDto> assignments = schedule.getAssignments().stream()
                .map(this::toAssignmentDto)
                .toList();

        return ScheduleDto.builder()
                .id(schedule.getId())
                .year(schedule.getYear())
                .month(schedule.getMonth())
                .status(schedule.getStatus())
                .generatedAt(schedule.getGeneratedAt())
                .assignments(assignments)
                .build();
    }

    public ShiftAssignmentDto toAssignmentDto(ShiftAssignment assignment) {
        return ShiftAssignmentDto.builder()
                .id(assignment.getId())
                .doctorId(assignment.getDoctor().getId())
                .doctorName(assignment.getDoctor().getFirstName() + " " + assignment.getDoctor().getLastName())
                .date(assignment.getDate())
                .shiftType(assignment.getShiftType())
                .doctorColor(assignment.getDoctor().getColor())
                .forced(assignment.isForced())
                .warning(assignment.getWarning())
                .build();
    }
}
