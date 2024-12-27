package com.pp.smarthealth.service.impl;

import com.pp.smarthealth.model.Appointment;
import com.pp.smarthealth.model.Doctor;
import com.pp.smarthealth.model.Patient;
import com.pp.smarthealth.repository.AppointmentRepository;
import com.pp.smarthealth.repository.DoctorRepository;
import com.pp.smarthealth.repository.PatientRepository;
import com.pp.smarthealth.service.AppointmentService;
import com.pp.smarthealth.dto.AppointmentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Override
    public AppointmentDTO createAppointment(AppointmentDTO appointmentDTO) {
        Doctor doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        Patient patient = patientRepository.findById(appointmentDTO.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Appointment appointment = new Appointment();
        appointment.setDateTime(appointmentDTO.getDateTime());
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setStatus(appointmentDTO.getStatus());
        appointment.setNotes(appointmentDTO.getNotes());

        Appointment savedAppointment = appointmentRepository.save(appointment);

        return convertToDTO(savedAppointment);
    }

    @Override
    public AppointmentDTO getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        return convertToDTO(appointment);
    }

    @Override
    public List<AppointmentDTO> getAllAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        return appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentDTO updateAppointment(Long id, AppointmentDTO appointmentDTO) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointment.setDateTime(appointmentDTO.getDateTime());
        appointment.setStatus(appointmentDTO.getStatus());
        appointment.setNotes(appointmentDTO.getNotes());

        if (appointmentDTO.getDoctorId() != null) {
            Doctor doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));
            appointment.setDoctor(doctor);
        }

        if (appointmentDTO.getPatientId() != null) {
            Patient patient = patientRepository.findById(appointmentDTO.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            appointment.setPatient(patient);
        }

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        return convertToDTO(updatedAppointment);
    }

    @Override
    public void deleteAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointmentRepository.delete(appointment);
    }

    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO appointmentDTO = new AppointmentDTO();
        appointmentDTO.setId(appointment.getId());
        appointmentDTO.setDateTime(appointment.getDateTime());
        appointmentDTO.setDoctorId(appointment.getDoctor().getId());
        appointmentDTO.setPatientId(appointment.getPatient().getId());
        appointmentDTO.setStatus(appointment.getStatus());
        appointmentDTO.setNotes(appointment.getNotes());

        return appointmentDTO;
    }
}
