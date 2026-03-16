package com.berit.lids.springboot.application.serviceplan.repository;

import com.berit.lids.springboot.application.serviceplan.domain.entity.ShiftPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ShiftPreferenceRepository extends JpaRepository<ShiftPreference, Long> {

    List<ShiftPreference> findByDoctorIdAndDateBetween(Long doctorId, LocalDate from, LocalDate to);

    List<ShiftPreference> findByDateBetween(LocalDate from, LocalDate to);

    Optional<ShiftPreference> findByDoctorIdAndDate(Long doctorId, LocalDate date);
}
