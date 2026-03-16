package com.berit.lids.springboot.application.serviceplan.controller;

import com.berit.lids.springboot.application.serviceplan.dto.*;
import com.berit.lids.springboot.application.serviceplan.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/conflicts")
    public List<ConflictDto> getConflicts(@RequestParam int year, @RequestParam int month) {
        return scheduleService.detectConflicts(year, month);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleDto generate(@Valid @RequestBody GenerateScheduleRequest request) {
        return scheduleService.generateSchedule(request.getYear(), request.getMonth(), request.getOverrides());
    }

    @GetMapping
    public ScheduleDto getSchedule(@RequestParam int year, @RequestParam int month) {
        return scheduleService.getSchedule(year, month);
    }

    @PutMapping("/{id}/assignments/{assignmentId}")
    public ShiftAssignmentDto swapAssignment(
            @PathVariable Long id,
            @PathVariable Long assignmentId,
            @Valid @RequestBody SwapAssignmentRequest request) {
        return scheduleService.swapAssignment(id, assignmentId, request.getNewDoctorId());
    }

    @PostMapping("/{id}/swap-two")
    public ScheduleDto swapTwo(@PathVariable Long id, @Valid @RequestBody SwapTwoRequest request) {
        return scheduleService.swapTwo(id, request.getAssignmentId1(), request.getAssignmentId2());
    }

    @PostMapping("/{id}/publish")
    public ScheduleDto publish(@PathVariable Long id) {
        return scheduleService.publishSchedule(id);
    }

    @GetMapping("/{id}/summary")
    public ScheduleSummaryDto summary(@PathVariable Long id) {
        return scheduleService.getSummary(id);
    }
}
