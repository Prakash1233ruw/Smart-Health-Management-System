package com.pp.smarthealth.service;

import com.pp.smarthealth.dto.AppointmentDTO;
import com.pp.smarthealth.dto.DoctorDTO;
import com.pp.smarthealth.model.Appointment;
import com.pp.smarthealth.model.Doctor;

import java.util.List;

public interface DoctorService {
    DoctorDTO saveDoctor(Doctor doctor);
    DoctorDTO updateDoctor(Long id, Doctor doctorDetails);
    void deleteDoctor(Long id);
    DoctorDTO getDoctorById(Long id);
    List<DoctorDTO> getAllDoctors();
	List<AppointmentDTO> getUpcomingAppointments(Long doctorId);
}
