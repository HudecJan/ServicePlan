package com.berit.lids.springboot.application.serviceplan.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleSummaryDto {
    private int year;
    private int month;
    private Map<String, DoctorShiftCount> doctorCounts;
    private int maxShifts;
    private int minShifts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorShiftCount {
        private String doctorName;
        private int weekdayOnCall;
        private int weekendOnCall;
        private int weekendAssistant;
        private int totalOnCall;
    }
}
