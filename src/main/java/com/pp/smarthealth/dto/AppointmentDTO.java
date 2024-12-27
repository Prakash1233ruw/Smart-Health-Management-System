package com.pp.smarthealth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDTO {

    private Long id;
    private LocalDateTime dateTime;
    private Long doctorId;
    private Long patientId;
    private String status;
    private String notes;
}
