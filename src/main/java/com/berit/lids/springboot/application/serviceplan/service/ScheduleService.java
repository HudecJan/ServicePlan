package com.berit.lids.springboot.application.serviceplan.service;

import com.berit.lids.springboot.application.serviceplan.domain.entity.Doctor;
import com.berit.lids.springboot.application.serviceplan.domain.entity.MonthlySchedule;
import com.berit.lids.springboot.application.serviceplan.domain.entity.ShiftAssignment;
import com.berit.lids.springboot.application.serviceplan.domain.entity.ShiftPreference;
import com.berit.lids.springboot.application.serviceplan.domain.enums.ScheduleStatus;
import com.berit.lids.springboot.application.serviceplan.domain.enums.ShiftType;
import com.berit.lids.springboot.application.serviceplan.dto.*;
import com.berit.lids.springboot.application.serviceplan.mapper.ScheduleMapper;
import com.berit.lids.springboot.application.serviceplan.repository.DoctorRepository;
import com.berit.lids.springboot.application.serviceplan.repository.MonthlyScheduleRepository;
import com.berit.lids.springboot.application.serviceplan.repository.ShiftAssignmentRepository;
import com.berit.lids.springboot.application.serviceplan.repository.ShiftPreferenceRepository;
import com.berit.lids.springboot.application.serviceplan.scheduler.ShiftScheduler;
import com.berit.lids.springboot.application.serviceplan.scheduler.ScheduleConstraintValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.berit.lids.springboot.application.serviceplan.domain.enums.PreferenceType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final MonthlyScheduleRepository scheduleRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final DoctorRepository doctorRepository;
    private final ShiftPreferenceRepository preferenceRepository;
    private final ShiftScheduler shiftScheduler;
    private final ScheduleConstraintValidator constraintValidator;
    private final ScheduleMapper scheduleMapper;

    private static final String[] SK_DAYS = {"", "pondelok", "utorok", "streda", "štvrtok", "piatok", "sobota", "nedeľa"};

    @Transactional(readOnly = true)
    public List<ConflictDto> detectConflicts(int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        List<ShiftPreference> preferences = preferenceRepository.findByDateBetween(from, to);
        List<Doctor> doctors = doctorRepository.findByActiveTrue();

        // Index preferences by date
        Map<LocalDate, List<ShiftPreference>> prefsByDate = preferences.stream()
                .collect(Collectors.groupingBy(ShiftPreference::getDate));

        List<ConflictDto> conflicts = new ArrayList<>();

        for (int day = 1; day <= from.lengthOfMonth(); day++) {
            LocalDate date = from.withDayOfMonth(day);
            boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            int slotsNeeded = isWeekend ? 2 : 1;

            List<ShiftPreference> dayPrefs = prefsByDate.getOrDefault(date, List.of());
            Map<Long, ShiftPreference> prefByDoctor = dayPrefs.stream()
                    .collect(Collectors.toMap(p -> p.getDoctor().getId(), p -> p, (a, b) -> a));

            // WANT conflicts: multiple doctors want same day
            List<ShiftPreference> wants = dayPrefs.stream()
                    .filter(p -> p.getPreferenceType() == PreferenceType.WANT_ON_CALL)
                    .toList();
            if (wants.size() > 1) {
                conflicts.add(ConflictDto.builder()
                        .date(date)
                        .dayName(SK_DAYS[date.getDayOfWeek().getValue()])
                        .weekend(isWeekend)
                        .type("WANT_CONFLICT")
                        .slotsNeeded(slotsNeeded)
                        .availableCount((int) doctors.stream()
                                .filter(d -> {
                                    ShiftPreference p = prefByDoctor.get(d.getId());
                                    return p == null || p.getPreferenceType() != PreferenceType.CANNOT_ON_CALL;
                                }).count())
                        .candidates(wants.stream()
                                .map(p -> ConflictDto.DoctorOption.builder()
                                        .doctorId(p.getDoctor().getId())
                                        .doctorName(p.getDoctor().getFirstName() + " " + p.getDoctor().getLastName())
                                        .color(p.getDoctor().getColor())
                                        .note(p.getNote())
                                        .hasCannot(false)
                                        .build())
                                .toList())
                        .build());
            }

            // SHORTAGE: not enough doctors available (too many CANNOT)
            long cannotCount = dayPrefs.stream()
                    .filter(p -> p.getPreferenceType() == PreferenceType.CANNOT_ON_CALL)
                    .count();
            int available = doctors.size() - (int) cannotCount;

            if (available < slotsNeeded) {
                // List all doctors, marking who has CANNOT
                conflicts.add(ConflictDto.builder()
                        .date(date)
                        .dayName(SK_DAYS[date.getDayOfWeek().getValue()])
                        .weekend(isWeekend)
                        .type("SHORTAGE")
                        .slotsNeeded(slotsNeeded)
                        .availableCount(available)
                        .candidates(doctors.stream()
                                .map(d -> {
                                    ShiftPreference pref = prefByDoctor.get(d.getId());
                                    boolean hasCannot = pref != null && pref.getPreferenceType() == PreferenceType.CANNOT_ON_CALL;
                                    return ConflictDto.DoctorOption.builder()
                                            .doctorId(d.getId())
                                            .doctorName(d.getFirstName() + " " + d.getLastName())
                                            .color(d.getColor())
                                            .note(hasCannot && pref.getNote() != null ? pref.getNote() : null)
                                            .hasCannot(hasCannot)
                                            .build();
                                })
                                .toList())
                        .build());
            }
        }
        conflicts.sort(Comparator.comparing(ConflictDto::getDate));
        return conflicts;
    }

    @Transactional
    public ScheduleDto generateSchedule(int year, int month, Map<String, Long> overrides) {
        List<Doctor> doctors = doctorRepository.findByActiveTrue();
        if (doctors.size() < 2) {
            throw new IllegalStateException("Potrebujete aspoň 2 aktívnych lekárov na generovanie rozvrhu");
        }

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        List<ShiftPreference> preferences = preferenceRepository.findByDateBetween(from, to);

        // Reuse existing draft or create new
        MonthlySchedule schedule = scheduleRepository.findByYearAndMonth(year, month).orElse(null);

        if (schedule != null) {
            if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
                throw new IllegalStateException("Rozvrh pre " + year + "/" + month + " je už publikovaný. Nie je možné pregenerovať.");
            }
            schedule.getAssignments().clear();
            schedule.setGeneratedAt(LocalDateTime.now());
            scheduleRepository.saveAndFlush(schedule);
        } else {
            schedule = MonthlySchedule.builder()
                    .year(year)
                    .month(month)
                    .status(ScheduleStatus.DRAFT)
                    .generatedAt(LocalDateTime.now())
                    .build();
            schedule = scheduleRepository.saveAndFlush(schedule);
        }

        List<ShiftAssignment> assignments = shiftScheduler.generate(
                schedule, doctors, preferences, overrides != null ? overrides : Map.of());
        schedule.getAssignments().addAll(assignments);
        schedule = scheduleRepository.save(schedule);

        return scheduleMapper.toDto(schedule);
    }

    @Transactional(readOnly = true)
    public ScheduleDto getSchedule(int year, int month) {
        MonthlySchedule schedule = scheduleRepository.findByYearAndMonth(year, month)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found for " + year + "/" + month));
        return scheduleMapper.toDto(schedule);
    }

    @Transactional
    public ScheduleDto publishSchedule(Long scheduleId) {
        MonthlySchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        schedule.setStatus(ScheduleStatus.PUBLISHED);
        return scheduleMapper.toDto(scheduleRepository.save(schedule));
    }

    @Transactional
    public ShiftAssignmentDto swapAssignment(Long scheduleId, Long assignmentId, Long newDoctorId) {
        MonthlySchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot modify a published schedule");
        }

        ShiftAssignment assignment = schedule.getAssignments().stream()
                .filter(a -> a.getId().equals(assignmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        Doctor newDoctor = doctorRepository.findById(newDoctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + newDoctorId));

        Doctor oldDoctor = assignment.getDoctor();
        assignment.setDoctor(newDoctor);

        // Validate constraints after swap
        LocalDate from = LocalDate.of(schedule.getYear(), schedule.getMonth(), 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        List<ShiftPreference> preferences = preferenceRepository.findByDateBetween(from, to);
        List<String> violations = constraintValidator.validate(schedule.getAssignments(), preferences);

        if (!violations.isEmpty()) {
            assignment.setDoctor(oldDoctor); // rollback
            throw new IllegalStateException("Swap violates constraints: " + String.join("; ", violations));
        }

        assignmentRepository.save(assignment);
        return scheduleMapper.toAssignmentDto(assignment);
    }

    @Transactional
    public ScheduleDto swapTwo(Long scheduleId, Long assignmentId1, Long assignmentId2) {
        MonthlySchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Rozvrh nenájdený: " + scheduleId));

        if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Nie je možné upravovať publikovaný rozvrh");
        }

        ShiftAssignment a1 = schedule.getAssignments().stream()
                .filter(a -> a.getId().equals(assignmentId1))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Priradenie nenájdené: " + assignmentId1));

        ShiftAssignment a2 = schedule.getAssignments().stream()
                .filter(a -> a.getId().equals(assignmentId2))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Priradenie nenájdené: " + assignmentId2));

        // Swap doctors
        Doctor doc1 = a1.getDoctor();
        Doctor doc2 = a2.getDoctor();
        a1.setDoctor(doc2);
        a2.setDoctor(doc1);

        // Clear forced flags since this is a manual decision
        a1.setForced(false);
        a1.setWarning(null);
        a2.setForced(false);
        a2.setWarning(null);

        // Validate only hard constraints (consecutive shifts, day-off) — skip preference checks
        // since swap is an explicit manual decision
        List<String> violations = constraintValidator.validateHardOnly(schedule.getAssignments());

        if (!violations.isEmpty()) {
            a1.setDoctor(doc1);
            a2.setDoctor(doc2);
            throw new IllegalStateException("Výmena porušuje pravidlá: " + String.join("; ", violations));
        }

        assignmentRepository.save(a1);
        assignmentRepository.save(a2);
        return scheduleMapper.toDto(schedule);
    }

    @Transactional(readOnly = true)
    public ScheduleSummaryDto getSummary(Long scheduleId) {
        MonthlySchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        Map<String, ScheduleSummaryDto.DoctorShiftCount> counts = new LinkedHashMap<>();

        for (ShiftAssignment a : schedule.getAssignments()) {
            String key = a.getDoctor().getId().toString();
            String name = a.getDoctor().getFirstName() + " " + a.getDoctor().getLastName();

            ScheduleSummaryDto.DoctorShiftCount count = counts.computeIfAbsent(key,
                    k -> ScheduleSummaryDto.DoctorShiftCount.builder().doctorName(name).build());

            switch (a.getShiftType()) {
                case ON_CALL_WEEKDAY_16H -> {
                    count.setWeekdayOnCall(count.getWeekdayOnCall() + 1);
                    count.setTotalOnCall(count.getTotalOnCall() + 1);
                }
                case ON_CALL_WEEKEND_24H -> {
                    count.setWeekendOnCall(count.getWeekendOnCall() + 1);
                    count.setTotalOnCall(count.getTotalOnCall() + 1);
                }
                case ASSISTANT_WEEKEND -> count.setWeekendAssistant(count.getWeekendAssistant() + 1);
            }
        }

        int max = counts.values().stream().mapToInt(ScheduleSummaryDto.DoctorShiftCount::getTotalOnCall).max().orElse(0);
        int min = counts.values().stream().mapToInt(ScheduleSummaryDto.DoctorShiftCount::getTotalOnCall).min().orElse(0);

        return ScheduleSummaryDto.builder()
                .year(schedule.getYear())
                .month(schedule.getMonth())
                .doctorCounts(counts)
                .maxShifts(max)
                .minShifts(min)
                .build();
    }
}
