package com.berit.lids.springboot.application.serviceplan.scheduler;

import com.berit.lids.springboot.application.serviceplan.domain.entity.Doctor;
import com.berit.lids.springboot.application.serviceplan.domain.entity.MonthlySchedule;
import com.berit.lids.springboot.application.serviceplan.domain.entity.ShiftAssignment;
import com.berit.lids.springboot.application.serviceplan.domain.entity.ShiftPreference;
import com.berit.lids.springboot.application.serviceplan.domain.enums.PreferenceType;
import com.berit.lids.springboot.application.serviceplan.domain.enums.ShiftType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ShiftScheduler {

    private record ForcedPick(Doctor doctor, String warning) {}

    public List<ShiftAssignment> generate(MonthlySchedule schedule, List<Doctor> doctors,
                                           List<ShiftPreference> preferences,
                                           Map<String, Long> overrides) {
        Map<LocalDate, Map<Long, PreferenceType>> prefMap = new HashMap<>();
        for (ShiftPreference pref : preferences) {
            prefMap.computeIfAbsent(pref.getDate(), k -> new HashMap<>())
                    .put(pref.getDoctor().getId(), pref.getPreferenceType());
        }

        // Apply overrides:
        // - For WANT conflicts: winner keeps WANT, losers become neutral
        // - For SHORTAGE: winner's CANNOT is removed so they become eligible
        for (Map.Entry<String, Long> override : overrides.entrySet()) {
            LocalDate date = LocalDate.parse(override.getKey());
            Long winnerId = override.getValue();
            Map<Long, PreferenceType> dayPrefs = prefMap.computeIfAbsent(date, k -> new HashMap<>());

            // Remove WANT from losers
            dayPrefs.entrySet().removeIf(e ->
                    e.getValue() == PreferenceType.WANT_ON_CALL && !e.getKey().equals(winnerId));

            // If winner had CANNOT, remove it (user explicitly chose them)
            if (dayPrefs.get(winnerId) == PreferenceType.CANNOT_ON_CALL) {
                dayPrefs.remove(winnerId);
            }

            // Ensure winner has WANT
            dayPrefs.put(winnerId, PreferenceType.WANT_ON_CALL);
        }

        Map<Long, Integer> onCallCount = new HashMap<>();
        Map<Long, Integer> weekendOnCallCount = new HashMap<>();
        Map<Long, Integer> assistantCount = new HashMap<>();
        Map<Long, LocalDate> lastOnCallDate = new HashMap<>();
        Map<Long, LocalDate> lastAssistantDate = new HashMap<>();
        Set<String> dayOffEntries = new HashSet<>();
        Set<Long> fridayOnCallDoctors = new HashSet<>();

        for (Doctor d : doctors) {
            onCallCount.put(d.getId(), 0);
            weekendOnCallCount.put(d.getId(), 0);
            assistantCount.put(d.getId(), 0);
        }

        // Extra shifts: doctorId -> how many extra shifts they want
        Map<Long, Integer> extraShiftsWanted = doctors.stream()
                .filter(d -> d.getExtraShifts() > 0)
                .collect(Collectors.toMap(Doctor::getId, Doctor::getExtraShifts));

        List<ShiftAssignment> allAssignments = new ArrayList<>();
        LocalDate firstDay = LocalDate.of(schedule.getYear(), schedule.getMonth(), 1);
        int daysInMonth = firstDay.lengthOfMonth();
        Random random = new Random(schedule.getYear() * 100L + schedule.getMonth());

        List<LocalDate> weekendDates = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = firstDay.withDayOfMonth(day);
            if (isWeekend(date)) weekendDates.add(date);
        }

        // PHASE 1: Pre-assign mandatory weekend on-call (1 per doctor)
        Map<Long, LocalDate> mandatoryWeekend = preAssignMandatoryWeekends(
                doctors, weekendDates, prefMap, random);

        log.info("Mandatory weekend assignments: {}", mandatoryWeekend.entrySet().stream()
                .map(e -> doctorName(doctors, e.getKey()) + " -> " + e.getValue())
                .collect(Collectors.joining(", ")));

        // PHASE 2: Process all days
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = firstDay.withDayOfMonth(day);

            if (isWeekend(date)) {
                // Weekend on-call 24h
                Doctor mandatoryDoc = findMandatoryDoctor(mandatoryWeekend, doctors, date);
                Doctor onCallDoc;
                boolean forced = false;
                String warning = null;

                if (mandatoryDoc != null && isEligible(mandatoryDoc, date, prefMap, lastOnCallDate, dayOffEntries)) {
                    onCallDoc = mandatoryDoc;
                } else {
                    onCallDoc = pickWeekendDoctor(doctors, date, prefMap, onCallCount, weekendOnCallCount,
                            lastOnCallDate, dayOffEntries, null, extraShiftsWanted, random);
                }

                if (onCallDoc == null) {
                    ForcedPick fp = pickWithRelaxedConstraints(doctors, date, prefMap, lastOnCallDate, dayOffEntries, null);
                    if (fp != null) {
                        onCallDoc = fp.doctor;
                        forced = true;
                        warning = fp.warning;
                    }
                }

                if (onCallDoc != null) {
                    allAssignments.add(createAssignment(schedule, onCallDoc, date, ShiftType.ON_CALL_WEEKEND_24H, forced, warning));
                    updateState(onCallDoc, date, onCallCount, lastOnCallDate, dayOffEntries);
                    weekendOnCallCount.merge(onCallDoc.getId(), 1, Integer::sum);
                }

                // Weekend assistant
                Doctor assistantDoc = pickAssistant(doctors, date, prefMap, assistantCount,
                        lastOnCallDate, lastAssistantDate, dayOffEntries, onCallDoc, random);
                boolean aForced = false;
                String aWarning = null;

                if (assistantDoc == null) {
                    ForcedPick fp = pickWithRelaxedConstraints(doctors, date, prefMap, lastOnCallDate, dayOffEntries, onCallDoc);
                    if (fp != null) {
                        assistantDoc = fp.doctor;
                        aForced = true;
                        aWarning = fp.warning;
                    }
                }

                if (assistantDoc != null) {
                    allAssignments.add(createAssignment(schedule, assistantDoc, date, ShiftType.ASSISTANT_WEEKEND, aForced, aWarning));
                    assistantCount.merge(assistantDoc.getId(), 1, Integer::sum);
                    lastAssistantDate.put(assistantDoc.getId(), date);
                    // Day off after assistant too
                    dayOffEntries.add(assistantDoc.getId() + "_" + date.plusDays(1));
                }
            } else {
                // Weekday on-call 16h
                boolean isFriday = date.getDayOfWeek() == DayOfWeek.FRIDAY;
                Doctor onCallDoc = pickWeekdayDoctor(doctors, date, prefMap, onCallCount,
                        lastOnCallDate, dayOffEntries, extraShiftsWanted, fridayOnCallDoctors, isFriday, random);
                boolean forced = false;
                String warning = null;

                if (onCallDoc == null) {
                    ForcedPick fp = pickWithRelaxedConstraints(doctors, date, prefMap, lastOnCallDate, dayOffEntries, null);
                    if (fp != null) {
                        onCallDoc = fp.doctor;
                        forced = true;
                        warning = fp.warning;
                    }
                }

                if (onCallDoc != null) {
                    allAssignments.add(createAssignment(schedule, onCallDoc, date, ShiftType.ON_CALL_WEEKDAY_16H, forced, warning));
                    if (isFriday) fridayOnCallDoctors.add(onCallDoc.getId());
                    updateState(onCallDoc, date, onCallCount, lastOnCallDate, dayOffEntries);
                }
            }
        }

        logFairness(doctors, onCallCount, weekendOnCallCount, assistantCount);
        return allAssignments;
    }

    private ForcedPick pickWithRelaxedConstraints(List<Doctor> doctors, LocalDate date,
                                                    Map<LocalDate, Map<Long, PreferenceType>> prefMap,
                                                    Map<Long, LocalDate> lastOnCallDate,
                                                    Set<String> dayOffEntries,
                                                    Doctor excluded) {
        Map<Long, PreferenceType> dayPrefs = prefMap.getOrDefault(date, Map.of());

        // Level 1: skip day-off rule but respect CANNOT
        List<Doctor> candidates = doctors.stream()
                .filter(d -> excluded == null || !d.getId().equals(excluded.getId()))
                .filter(d -> dayPrefs.get(d.getId()) != PreferenceType.CANNOT_ON_CALL)
                .collect(Collectors.toList());

        if (!candidates.isEmpty()) {
            // Prefer those not on day-off, then not consecutive
            candidates.sort(Comparator
                    .comparingInt((Doctor d) -> dayOffEntries.contains(d.getId() + "_" + date) ? 1 : 0)
                    .thenComparingInt(d -> {
                        LocalDate last = lastOnCallDate.get(d.getId());
                        return (last != null && last.equals(date.minusDays(1))) ? 1 : 0;
                    }));

            Doctor picked = candidates.get(0);
            String name = picked.getFirstName() + " " + picked.getLastName();

            if (dayOffEntries.contains(picked.getId() + "_" + date)) {
                return new ForcedPick(picked, name + " priradený/á napriek dňu voľna — nedostatok lekárov");
            } else {
                return new ForcedPick(picked, name + " priradený/á napriek po sebe idúcim službám — nedostatok lekárov");
            }
        }

        // Level 2: even CANNOT (absolute last resort)
        candidates = doctors.stream()
                .filter(d -> excluded == null || !d.getId().equals(excluded.getId()))
                .collect(Collectors.toList());

        if (!candidates.isEmpty()) {
            Doctor picked = candidates.get(0);
            String name = picked.getFirstName() + " " + picked.getLastName();
            return new ForcedPick(picked, "NÚDZOVÉ: " + name + " priradený/á napriek požiadavke NEMÔŽEM — kritický nedostatok lekárov!");
        }

        return null;
    }

    private Map<Long, LocalDate> preAssignMandatoryWeekends(List<Doctor> doctors,
                                                              List<LocalDate> weekendDates,
                                                              Map<LocalDate, Map<Long, PreferenceType>> prefMap,
                                                              Random random) {
        Map<Long, LocalDate> result = new HashMap<>();
        Set<LocalDate> usedDates = new HashSet<>();

        List<Doctor> shuffled = new ArrayList<>(doctors);
        Collections.shuffle(shuffled, random);

        for (Doctor doc : shuffled) {
            for (LocalDate wDate : weekendDates) {
                if (usedDates.contains(wDate)) continue;
                Map<Long, PreferenceType> dayPrefs = prefMap.getOrDefault(wDate, Map.of());
                if (dayPrefs.get(doc.getId()) == PreferenceType.CANNOT_ON_CALL) continue;

                boolean conflict = result.entrySet().stream()
                        .filter(e -> e.getKey().equals(doc.getId()))
                        .anyMatch(e -> Math.abs(e.getValue().toEpochDay() - wDate.toEpochDay()) <= 1);
                if (conflict) continue;

                result.put(doc.getId(), wDate);
                usedDates.add(wDate);
                break;
            }
        }
        return result;
    }

    private Doctor pickWeekendDoctor(List<Doctor> doctors, LocalDate date,
                                      Map<LocalDate, Map<Long, PreferenceType>> prefMap,
                                      Map<Long, Integer> onCallCount,
                                      Map<Long, Integer> weekendOnCallCount,
                                      Map<Long, LocalDate> lastOnCallDate,
                                      Set<String> dayOffEntries,
                                      Doctor excluded,
                                      Map<Long, Integer> extraShiftsWanted,
                                      Random random) {
        List<Doctor> candidates = filterCandidates(doctors, date, prefMap, lastOnCallDate, dayOffEntries, excluded);
        if (candidates.isEmpty()) return null;

        Map<Long, PreferenceType> dayPrefs = prefMap.getOrDefault(date, Map.of());

        List<Doctor> noWeekendYet = candidates.stream()
                .filter(d -> weekendOnCallCount.getOrDefault(d.getId(), 0) == 0)
                .collect(Collectors.toList());
        List<Doctor> wantDoctors = candidates.stream()
                .filter(d -> dayPrefs.get(d.getId()) == PreferenceType.WANT_ON_CALL)
                .collect(Collectors.toList());

        List<Doctor> pool = noWeekendYet.stream().filter(wantDoctors::contains).collect(Collectors.toList());
        if (!pool.isEmpty()) return pickFromPool(pool, onCallCount, random);
        if (!noWeekendYet.isEmpty()) return pickFromPool(noWeekendYet, onCallCount, random);
        if (!wantDoctors.isEmpty()) return pickFromPool(wantDoctors, onCallCount, random);

        return pickFromPoolWithExtra(candidates, onCallCount, extraShiftsWanted, random);
    }

    private Doctor pickWeekdayDoctor(List<Doctor> doctors, LocalDate date,
                                      Map<LocalDate, Map<Long, PreferenceType>> prefMap,
                                      Map<Long, Integer> countMap,
                                      Map<Long, LocalDate> lastOnCallDate,
                                      Set<String> dayOffEntries,
                                      Map<Long, Integer> extraShiftsWanted,
                                      Set<Long> fridayOnCallDoctors,
                                      boolean isFriday,
                                      Random random) {
        List<Doctor> candidates = filterCandidates(doctors, date, prefMap, lastOnCallDate, dayOffEntries, null);
        if (candidates.isEmpty()) return null;

        // Friday rule: exclude doctors who already had a Friday shift this month
        if (isFriday && !fridayOnCallDoctors.isEmpty()) {
            List<Doctor> nonFridayCandidates = candidates.stream()
                    .filter(d -> !fridayOnCallDoctors.contains(d.getId()))
                    .collect(Collectors.toList());
            if (!nonFridayCandidates.isEmpty()) {
                candidates = nonFridayCandidates;
            }
            // If all remaining had Friday already, allow anyway (fallback)
        }

        Map<Long, PreferenceType> dayPrefs = prefMap.getOrDefault(date, Map.of());
        List<Doctor> wantDoctors = candidates.stream()
                .filter(d -> dayPrefs.get(d.getId()) == PreferenceType.WANT_ON_CALL)
                .collect(Collectors.toList());
        if (!wantDoctors.isEmpty()) return pickFromPool(wantDoctors, countMap, random);

        // Use adjusted count: extra doctors get their count reduced by their bonus
        // This makes them appear as if they have fewer shifts, so they get picked more
        return pickFromPoolWithExtra(candidates, countMap, extraShiftsWanted, random);
    }

    private Doctor pickAssistant(List<Doctor> doctors, LocalDate date,
                                  Map<LocalDate, Map<Long, PreferenceType>> prefMap,
                                  Map<Long, Integer> assistantCount,
                                  Map<Long, LocalDate> lastOnCallDate,
                                  Map<Long, LocalDate> lastAssistantDate,
                                  Set<String> dayOffEntries,
                                  Doctor excludedOnCall,
                                  Random random) {
        List<Doctor> candidates = filterCandidates(doctors, date, prefMap, lastOnCallDate, dayOffEntries, excludedOnCall);
        // Exclude doctors who had assistant shift the day before (no 2 consecutive assistants)
        candidates = candidates.stream()
                .filter(d -> {
                    LocalDate lastAst = lastAssistantDate.get(d.getId());
                    return lastAst == null || !lastAst.equals(date.minusDays(1));
                })
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            // Fallback: allow consecutive but prefer those without
            candidates = filterCandidates(doctors, date, prefMap, lastOnCallDate, dayOffEntries, excludedOnCall);
        }
        if (candidates.isEmpty()) return null;
        return pickFromPool(candidates, assistantCount, random);
    }

    /**
     * Check if doctor still has extra shifts remaining.
     * Extra doctor's target = min count of non-extra doctors + extraWanted.
     * So if non-extra doctors have 4 shifts min, and Tomáš wants +3, his target is 7.
     */
    private boolean hasExtraRemaining(Doctor d, Map<Long, Integer> extraShiftsWanted,
                                       Map<Long, Integer> onCallCount, List<Doctor> allDoctors) {
        int wanted = extraShiftsWanted.getOrDefault(d.getId(), 0);
        if (wanted == 0) return false;
        int current = onCallCount.getOrDefault(d.getId(), 0);
        // Target: what non-extra doctors currently have (min) + extra wanted
        int nonExtraMin = allDoctors.stream()
                .filter(doc -> extraShiftsWanted.getOrDefault(doc.getId(), 0) == 0)
                .mapToInt(doc -> onCallCount.getOrDefault(doc.getId(), 0))
                .min().orElse(current);
        return current < nonExtraMin + wanted;
    }

    private List<Doctor> getExtraCandidates(List<Doctor> candidates, Map<Long, Integer> extraShiftsWanted,
                                             Map<Long, Integer> onCallCount, List<Doctor> allDoctors) {
        return candidates.stream()
                .filter(d -> hasExtraRemaining(d, extraShiftsWanted, onCallCount, allDoctors))
                .collect(Collectors.toList());
    }

    /**
     * Pick from pool using adjusted counts: extra doctors get count - extraWanted,
     * so they appear to have fewer shifts and get picked more often.
     */
    private Doctor pickFromPoolWithExtra(List<Doctor> pool, Map<Long, Integer> countMap,
                                          Map<Long, Integer> extraShiftsWanted, Random random) {
        // Adjusted count: actual - extra bonus (but minimum 0)
        pool.sort(Comparator.comparingInt(d -> {
            int actual = countMap.getOrDefault(d.getId(), 0);
            int extra = extraShiftsWanted.getOrDefault(d.getId(), 0);
            return actual - extra; // extra doctors appear to have fewer shifts
        }));

        int minAdjusted = countMap.getOrDefault(pool.get(0).getId(), 0)
                - extraShiftsWanted.getOrDefault(pool.get(0).getId(), 0);
        List<Doctor> topTier = pool.stream()
                .filter(d -> (countMap.getOrDefault(d.getId(), 0)
                        - extraShiftsWanted.getOrDefault(d.getId(), 0)) == minAdjusted)
                .collect(Collectors.toList());
        return topTier.get(random.nextInt(topTier.size()));
    }

    private Doctor pickFromPool(List<Doctor> pool, Map<Long, Integer> countMap, Random random) {
        pool.sort(Comparator.comparingInt(d -> countMap.getOrDefault(d.getId(), 0)));
        int minCount = countMap.getOrDefault(pool.get(0).getId(), 0);
        List<Doctor> topTier = pool.stream()
                .filter(d -> countMap.getOrDefault(d.getId(), 0) == minCount)
                .collect(Collectors.toList());
        return topTier.get(random.nextInt(topTier.size()));
    }

    private boolean isEligible(Doctor doctor, LocalDate date,
                                Map<LocalDate, Map<Long, PreferenceType>> prefMap,
                                Map<Long, LocalDate> lastOnCallDate,
                                Set<String> dayOffEntries) {
        Map<Long, PreferenceType> dayPrefs = prefMap.getOrDefault(date, Map.of());
        if (dayPrefs.get(doctor.getId()) == PreferenceType.CANNOT_ON_CALL) return false;
        if (dayOffEntries.contains(doctor.getId() + "_" + date)) return false;
        LocalDate lastDate = lastOnCallDate.get(doctor.getId());
        return lastDate == null || !lastDate.equals(date.minusDays(1));
    }

    private List<Doctor> filterCandidates(List<Doctor> doctors, LocalDate date,
                                           Map<LocalDate, Map<Long, PreferenceType>> prefMap,
                                           Map<Long, LocalDate> lastOnCallDate,
                                           Set<String> dayOffEntries,
                                           Doctor excluded) {
        Map<Long, PreferenceType> dayPrefs = prefMap.getOrDefault(date, Map.of());
        return doctors.stream()
                .filter(d -> excluded == null || !d.getId().equals(excluded.getId()))
                .filter(d -> dayPrefs.get(d.getId()) != PreferenceType.CANNOT_ON_CALL)
                .filter(d -> !dayOffEntries.contains(d.getId() + "_" + date))
                .filter(d -> {
                    LocalDate lastDate = lastOnCallDate.get(d.getId());
                    return lastDate == null || !lastDate.equals(date.minusDays(1));
                })
                .collect(Collectors.toList());
    }

    private void updateState(Doctor doctor, LocalDate date,
                              Map<Long, Integer> onCallCount,
                              Map<Long, LocalDate> lastOnCallDate,
                              Set<String> dayOffEntries) {
        onCallCount.merge(doctor.getId(), 1, Integer::sum);
        lastOnCallDate.put(doctor.getId(), date);
        dayOffEntries.add(doctor.getId() + "_" + date.plusDays(1));
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private Doctor findMandatoryDoctor(Map<Long, LocalDate> mandatory, List<Doctor> doctors, LocalDate date) {
        return mandatory.entrySet().stream()
                .filter(e -> e.getValue().equals(date))
                .findFirst()
                .flatMap(e -> doctors.stream().filter(d -> d.getId().equals(e.getKey())).findFirst())
                .orElse(null);
    }

    private String doctorName(List<Doctor> doctors, Long id) {
        return doctors.stream().filter(d -> d.getId().equals(id))
                .findFirst().map(d -> d.getFirstName() + " " + d.getLastName()).orElse("?");
    }

    private ShiftAssignment createAssignment(MonthlySchedule schedule, Doctor doctor,
                                              LocalDate date, ShiftType shiftType,
                                              boolean forced, String warning) {
        return ShiftAssignment.builder()
                .schedule(schedule)
                .doctor(doctor)
                .date(date)
                .shiftType(shiftType)
                .forced(forced)
                .warning(warning)
                .build();
    }

    private void logFairness(List<Doctor> doctors, Map<Long, Integer> onCallCount,
                              Map<Long, Integer> weekendOnCallCount, Map<Long, Integer> assistantCount) {
        log.info("=== Schedule Fairness Report ===");
        for (Doctor d : doctors) {
            log.info("{} {}: total on-call={}, weekend on-call={}, assistant={}, extra={}",
                    d.getFirstName(), d.getLastName(),
                    onCallCount.getOrDefault(d.getId(), 0),
                    weekendOnCallCount.getOrDefault(d.getId(), 0),
                    assistantCount.getOrDefault(d.getId(), 0),
                    d.getExtraShifts() > 0 ? "+" + d.getExtraShifts() : "no");
        }
    }
}
