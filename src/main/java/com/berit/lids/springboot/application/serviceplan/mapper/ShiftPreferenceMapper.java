package com.berit.lids.springboot.application.serviceplan.mapper;

import com.berit.lids.springboot.application.serviceplan.domain.entity.ShiftPreference;
import com.berit.lids.springboot.application.serviceplan.dto.ShiftPreferenceDto;
import org.springframework.stereotype.Component;

@Component
public class ShiftPreferenceMapper {

    public ShiftPreferenceDto toDto(ShiftPreference entity) {
        return ShiftPreferenceDto.builder()
                .id(entity.getId())
                .doctorId(entity.getDoctor().getId())
                .doctorName(entity.getDoctor().getFirstName() + " " + entity.getDoctor().getLastName())
                .date(entity.getDate())
                .preferenceType(entity.getPreferenceType())
                .note(entity.getNote())
                .build();
    }
}
