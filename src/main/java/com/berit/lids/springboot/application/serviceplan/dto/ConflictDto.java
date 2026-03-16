package com.berit.lids.springboot.application.serviceplan.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConflictDto {
    private LocalDate date;
    private String dayName;
    private boolean weekend;
    private String type; // "WANT_CONFLICT" or "SHORTAGE"
    private int slotsNeeded; // 1 for weekday, 2 for weekend (on-call + assistant)
    private int availableCount; // how many doctors are available (no CANNOT)
    private List<DoctorOption> candidates;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorOption {
        private Long doctorId;
        private String doctorName;
        private String color;
        private String note;
        private boolean hasCannot; // true if this doctor said CANNOT
    }
}
