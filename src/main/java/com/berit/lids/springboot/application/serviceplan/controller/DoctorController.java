package com.berit.lids.springboot.application.serviceplan.controller;

import com.berit.lids.springboot.application.serviceplan.dto.CreateDoctorRequest;
import com.berit.lids.springboot.application.serviceplan.dto.DoctorDto;
import com.berit.lids.springboot.application.serviceplan.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    public List<DoctorDto> getAll() {
        return doctorService.getAllActive();
    }

    @GetMapping("/{id}")
    public DoctorDto getById(@PathVariable Long id) {
        return doctorService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorDto create(@Valid @RequestBody CreateDoctorRequest request) {
        return doctorService.create(request);
    }

    @PutMapping("/{id}")
    public DoctorDto update(@PathVariable Long id, @Valid @RequestBody CreateDoctorRequest request) {
        return doctorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        doctorService.deactivate(id);
    }

    @PutMapping("/{id}/extra-shifts")
    public DoctorDto setExtraShifts(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return doctorService.setExtraShifts(id, body.getOrDefault("extraShifts", 0));
    }

    @PutMapping("/{id}/color")
    public DoctorDto updateColor(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return doctorService.updateColor(id, body.get("color"));
    }
}
