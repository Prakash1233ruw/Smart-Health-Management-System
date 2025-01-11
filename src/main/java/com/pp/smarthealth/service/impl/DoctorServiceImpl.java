package com.pp.smarthealth.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.pp.smarthealth.dto.AppointmentDTO;
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
    public List<AppointmentDTO> getUpcomingAppointments(Long doctorId) {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndDateTimeAfterOrderByDateTimeAsc(doctorId, now);
        return appointments.stream()
                           .map(appointment -> new AppointmentDTO(appointment.getId(), appointment.getDateTime(),appointment.getDoctor().getId(),appointment.getPatient().getId(),appointment.getStatus(),appointment.getNotes()))
                           .collect(Collectors.toList());
    }

    private DoctorDTO convertToDTO(Doctor doctor) {
        return new DoctorDTO(
            doctor.getId(),
            doctor.getName(),
            doctor.getSpecialization()
        );
    }

	@Override
	public List<AppointmentDTO> getUpcomingAppointments() {
		
		        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		        String username = authentication != null && authentication.isAuthenticated() ? authentication.getName() : null;

		        if (username == null) {
		            throw new RuntimeException("Unable to authenticate the user.");
		        }

		        Doctor doctor = doctorRepository.findByUsername(username)
		                .orElseThrow(() -> new RuntimeException("Doctor not found for username: " + username));

		        LocalDateTime now = LocalDateTime.now();
		        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndDateTimeAfterOrderByDateTimeAsc(doctor.getId(), now);
		        return appointments.stream()
		                           .map(appointment -> new AppointmentDTO(appointment.getId(), appointment.getDateTime(),appointment.getDoctor().getId(),appointment.getPatient().getId(),appointment.getStatus(),appointment.getNotes()))
		                           .collect(Collectors.toList());
		    
}
}
