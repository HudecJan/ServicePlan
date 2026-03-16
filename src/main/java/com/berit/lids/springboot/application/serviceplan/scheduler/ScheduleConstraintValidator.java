package com.berit.lids.springboot.application.serviceplan.scheduler;

import com.berit.lids.springboot.application.serviceplan.domain.entity.ShiftAssignment;
import com.berit.lids.springboot.application.serviceplan.domain.entity.ShiftPreference;
import com.berit.lids.springboot.application.serviceplan.domain.enums.PreferenceType;
import com.berit.lids.springboot.application.serviceplan.domain.enums.ShiftType;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ScheduleConstraintValidator {

    public List<String> validate(List<ShiftAssignment> assignments, List<ShiftPreference> preferences) {
        List<String> violations = new ArrayList<>();

        // Build lookup maps
        Map<LocalDate, List<ShiftAssignment>> byDate = assignments.stream()
                .collect(Collectors.groupingBy(ShiftAssignment::getDate));

        Map<Long, List<ShiftAssignment>> byDoctor = assignments.stream()
                .collect(Collectors.groupingBy(a -> a.getDoctor().getId()));

        Set<String> cannotDates = preferences.stream()
                .filter(p -> p.getPreferenceType() == PreferenceType.CANNOT_ON_CALL)
                .map(p -> p.getDoctor().getId() + "_" + p.getDate())
                .collect(Collectors.toSet());

        // Check each date
        for (Map.Entry<LocalDate, List<ShiftAssignment>> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<ShiftAssignment> dayAssignments = entry.getValue();
            boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == DayOfWeek.SUNDAY;

            if (isWeekend) {
                long onCall24 = dayAssignments.stream()
                        .filter(a -> a.getShiftType() == ShiftType.ON_CALL_WEEKEND_24H).count();
                long assistant = dayAssignments.stream()
                        .filter(a -> a.getShiftType() == ShiftType.ASSISTANT_WEEKEND).count();
                if (onCall24 != 1) {
                    violations.add(date + ": expected 1 ON_CALL_WEEKEND_24H, found " + onCall24);
                }
                if (assistant != 1) {
                    violations.add(date + ": expected 1 ASSISTANT_WEEKEND, found " + assistant);
                }
            } else {
                long onCall16 = dayAssignments.stream()
                        .filter(a -> a.getShiftType() == ShiftType.ON_CALL_WEEKDAY_16H).count();
                if (onCall16 != 1) {
                    violations.add(date + ": expected 1 ON_CALL_WEEKDAY_16H, found " + onCall16);
                }
            }
        }

        // Check per-doctor constraints
        for (Map.Entry<Long, List<ShiftAssignment>> entry : byDoctor.entrySet()) {
            Long doctorId = entry.getKey();
            List<ShiftAssignment> doctorAssignments = entry.getValue()
                    .stream().sorted(Comparator.comparing(ShiftAssignment::getDate)).toList();

            for (int i = 0; i < doctorAssignments.size(); i++) {
                ShiftAssignment current = doctorAssignments.get(i);
                String doctorName = current.getDoctor().getFirstName() + " " + current.getDoctor().getLastName();

                // Check CANNOT_ON_CALL
                String key = doctorId + "_" + current.getDate();
                if (cannotDates.contains(key)) {
                    violations.add(current.getDate() + ": " + doctorName + " has CANNOT_ON_CALL preference");
                }

                // Check consecutive on-call
                if (i + 1 < doctorAssignments.size()) {
                    ShiftAssignment next = doctorAssignments.get(i + 1);
                    if (isOnCall(current) && isOnCall(next)
                            && next.getDate().equals(current.getDate().plusDays(1))) {
                        violations.add(current.getDate() + "-" + next.getDate() + ": " + doctorName
                                + " has consecutive on-call shifts");
                    }
                }

                // Check day-off after on-call (next day should have no assignment for this doctor)
                if (isOnCall(current)) {
                    LocalDate nextDay = current.getDate().plusDays(1);
                    boolean hasNextDayAssignment = byDoctor.getOrDefault(doctorId, List.of()).stream()
                            .anyMatch(a -> a.getDate().equals(nextDay));
                    if (hasNextDayAssignment) {
                        violations.add(nextDay + ": " + doctorName + " should have day off after on-call on " + current.getDate());
                    }
                }
            }

            // Check same-day duplicate (doctor shouldn't have 2 on-call/assistant on same date)
            Map<LocalDate, List<ShiftAssignment>> doctorByDate = doctorAssignments.stream()
                    .collect(Collectors.groupingBy(ShiftAssignment::getDate));
            for (Map.Entry<LocalDate, List<ShiftAssignment>> dateEntry : doctorByDate.entrySet()) {
                if (dateEntry.getValue().size() > 1) {
                    violations.add(dateEntry.getKey() + ": " + entry.getValue().get(0).getDoctor().getFirstName()
                            + " has multiple shift assignments on same date");
                }
            }
        }

        return violations;
    }

    private boolean isOnCall(ShiftAssignment assignment) {
        return assignment.getShiftType() == ShiftType.ON_CALL_WEEKDAY_16H
                || assignment.getShiftType() == ShiftType.ON_CALL_WEEKEND_24H;
    }
}
