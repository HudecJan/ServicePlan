package com.berit.lids.springboot.application.serviceplan.service;

import com.berit.lids.springboot.application.serviceplan.domain.entity.Doctor;
import com.berit.lids.springboot.application.serviceplan.domain.entity.ShiftPreference;
import com.berit.lids.springboot.application.serviceplan.dto.CreatePreferenceRequest;
import com.berit.lids.springboot.application.serviceplan.dto.ShiftPreferenceDto;
import com.berit.lids.springboot.application.serviceplan.mapper.ShiftPreferenceMapper;
import com.berit.lids.springboot.application.serviceplan.repository.DoctorRepository;
import com.berit.lids.springboot.application.serviceplan.repository.ShiftPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftPreferenceService {

    private final ShiftPreferenceRepository preferenceRepository;
    private final DoctorRepository doctorRepository;
    private final ShiftPreferenceMapper preferenceMapper;

    public List<ShiftPreferenceDto> getByDoctorAndMonth(Long doctorId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return preferenceRepository.findByDoctorIdAndDateBetween(doctorId, from, to).stream()
                .map(preferenceMapper::toDto)
                .toList();
    }

    public List<ShiftPreferenceDto> getAllByMonth(int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return preferenceRepository.findByDateBetween(from, to).stream()
                .map(preferenceMapper::toDto)
                .toList();
    }

    @Transactional
    public ShiftPreferenceDto createOrUpdate(Long doctorId, CreatePreferenceRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        ShiftPreference preference = preferenceRepository
                .findByDoctorIdAndDate(doctorId, request.getDate())
                .orElse(ShiftPreference.builder()
                        .doctor(doctor)
                        .date(request.getDate())
                        .build());

        preference.setPreferenceType(request.getPreferenceType());
        preference.setNote(request.getNote());

        return preferenceMapper.toDto(preferenceRepository.save(preference));
    }

    @Transactional
    public void delete(Long doctorId, Long preferenceId) {
        ShiftPreference preference = preferenceRepository.findById(preferenceId)
                .orElseThrow(() -> new IllegalArgumentException("Preference not found: " + preferenceId));
        if (!preference.getDoctor().getId().equals(doctorId)) {
            throw new IllegalArgumentException("Preference does not belong to doctor: " + doctorId);
        }
        preferenceRepository.delete(preference);
    }
}
