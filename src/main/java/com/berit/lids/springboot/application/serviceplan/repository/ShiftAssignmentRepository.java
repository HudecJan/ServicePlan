package com.berit.lids.springboot.application.serviceplan.repository;

import com.berit.lids.springboot.application.serviceplan.domain.entity.ShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    List<ShiftAssignment> findByScheduleId(Long scheduleId);
}
