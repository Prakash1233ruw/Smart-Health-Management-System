package com.pp.smarthealth.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.pp.smarthealth.dto.DoctorDTO;
import com.pp.smarthealth.exception.ResourceNotFoundException;
import com.pp.smarthealth.model.Appointment;
import com.pp.smarthealth.model.Doctor;
import com.pp.smarthealth.repository.AppointmentRepository;
import com.pp.smarthealth.repository.DoctorRepository;
import com.pp.smarthealth.service.DoctorService;

@Service
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private AppointmentRepository appointmentRepository;


    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Override
    public DoctorDTO saveDoctor(Doctor doctor) {
        doctor.setPassword(passwordEncoder.encode(doctor.getPassword()));
        Doctor savedDoctor = doctorRepository.save(doctor);
        return convertToDTO(savedDoctor);
    }

    @Override
    public DoctorDTO updateDoctor(Long id, Doctor doctorDetails) {
        Doctor doctor = doctorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
        doctor.setName(doctorDetails.getName());
        doctor.setSpecialization(doctorDetails.getSpecialization());
        Doctor updatedDoctor = doctorRepository.save(doctor);
        return convertToDTO(updatedDoctor);
    }

    @Override
    public void deleteDoctor(Long id) {
        Doctor doctor = doctorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
        doctorRepository.delete(doctor);
    }

    @Override
    public DoctorDTO getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
        return convertToDTO(doctor);
    }

    @Override
    public List<DoctorDTO> getAllDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        return doctors.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    
   
    @Override
    public List<Appointment> getUpcomingAppointments(Long doctorId) {
        LocalDateTime now = LocalDateTime.now();
        return appointmentRepository.findByDoctorIdAndDateTimeAfterOrderByDateTimeAsc(doctorId, now);
    }

    private DoctorDTO convertToDTO(Doctor doctor) {
        return new DoctorDTO(
            doctor.getId(),
            doctor.getName(),
            doctor.getSpecialization()
        );
    }
}
