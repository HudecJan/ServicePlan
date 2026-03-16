package com.berit.lids.springboot.application.serviceplan.repository;

import com.berit.lids.springboot.application.serviceplan.domain.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByActiveTrue();

    boolean existsByEmail(String email);
}
