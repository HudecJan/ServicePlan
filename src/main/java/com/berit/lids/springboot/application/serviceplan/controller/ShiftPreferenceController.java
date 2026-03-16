package com.berit.lids.springboot.application.serviceplan.controller;

import com.berit.lids.springboot.application.serviceplan.dto.CreatePreferenceRequest;
import com.berit.lids.springboot.application.serviceplan.dto.ShiftPreferenceDto;
import com.berit.lids.springboot.application.serviceplan.service.ShiftPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShiftPreferenceController {

    private final ShiftPreferenceService preferenceService;

    @GetMapping("/api/doctors/{doctorId}/preferences")
    public List<ShiftPreferenceDto> getByDoctor(
            @PathVariable Long doctorId,
            @RequestParam int year,
            @RequestParam int month) {
        return preferenceService.getByDoctorAndMonth(doctorId, year, month);
    }

    @PostMapping("/api/doctors/{doctorId}/preferences")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftPreferenceDto create(
            @PathVariable Long doctorId,
            @Valid @RequestBody CreatePreferenceRequest request) {
        return preferenceService.createOrUpdate(doctorId, request);
    }

    @DeleteMapping("/api/doctors/{doctorId}/preferences/{prefId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long doctorId, @PathVariable Long prefId) {
        preferenceService.delete(doctorId, prefId);
    }

    @GetMapping("/api/preferences")
    public List<ShiftPreferenceDto> getAllByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return preferenceService.getAllByMonth(year, month);
    }
}
