package com.berit.lids.springboot.application.serviceplan.mapper;

import com.berit.lids.springboot.application.serviceplan.domain.entity.Doctor;
import com.berit.lids.springboot.application.serviceplan.dto.CreateDoctorRequest;
import com.berit.lids.springboot.application.serviceplan.dto.DoctorDto;
import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {

    public DoctorDto toDto(Doctor doctor) {
        return DoctorDto.builder()
                .id(doctor.getId())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .email(doctor.getEmail())
                .active(doctor.isActive())
                .extraShifts(doctor.getExtraShifts())
                .color(doctor.getColor())
                .build();
    }

    public Doctor toEntity(CreateDoctorRequest request) {
        return Doctor.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .active(true)
                .build();
    }
}
