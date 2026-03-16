package com.berit.lids.springboot.application.serviceplan.repository;

import com.berit.lids.springboot.application.serviceplan.domain.entity.MonthlySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyScheduleRepository extends JpaRepository<MonthlySchedule, Long> {

    Optional<MonthlySchedule> findByYearAndMonth(int year, int month);
}
