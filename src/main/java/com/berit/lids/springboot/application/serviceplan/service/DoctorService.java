package com.berit.lids.springboot.application.serviceplan.service;

import com.berit.lids.springboot.application.serviceplan.domain.entity.Doctor;
import com.berit.lids.springboot.application.serviceplan.dto.CreateDoctorRequest;
import com.berit.lids.springboot.application.serviceplan.dto.DoctorDto;
import com.berit.lids.springboot.application.serviceplan.mapper.DoctorMapper;
import com.berit.lids.springboot.application.serviceplan.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorMapper doctorMapper;

    public List<DoctorDto> getAllActive() {
        return doctorRepository.findByActiveTrue().stream()
                .map(doctorMapper::toDto)
                .toList();
    }

    public DoctorDto getById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + id));
        return doctorMapper.toDto(doctor);
    }

    @Transactional
    public DoctorDto create(CreateDoctorRequest request) {
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
        Doctor doctor = doctorMapper.toEntity(request);
        return doctorMapper.toDto(doctorRepository.save(doctor));
    }

    @Transactional
    public DoctorDto update(Long id, CreateDoctorRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + id));
        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setEmail(request.getEmail());
        return doctorMapper.toDto(doctorRepository.save(doctor));
    }

    @Transactional
    public void deactivate(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + id));
        doctor.setActive(false);
        doctorRepository.save(doctor);
    }

    @Transactional
    public DoctorDto setExtraShifts(Long id, int count) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + id));
        doctor.setExtraShifts(Math.max(0, count));
        return doctorMapper.toDto(doctorRepository.save(doctor));
    }

    @Transactional
    public DoctorDto updateColor(Long id, String color) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + id));
        doctor.setColor(color);
        return doctorMapper.toDto(doctorRepository.save(doctor));
    }
}
