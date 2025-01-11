package com.pp.smarthealth.service;

import com.pp.smarthealth.dto.AppointmentDTO;
import com.pp.smarthealth.model.Appointment;
import com.pp.smarthealth.model.Patient;

import java.util.List;

public interface AppointmentService {
    AppointmentDTO createAppointment(AppointmentDTO appointmentDTO);
    AppointmentDTO getAppointmentById(Long id);
    List<AppointmentDTO> getAllAppointments();
    AppointmentDTO updateAppointment(Long id, AppointmentDTO appointmentDTO);
    void deleteAppointment(Long id);
	void sendHealthCheckupAlerts();
	void sendAppointmentReminders();
	List<AppointmentDTO> findAppointmentsByPatient(Patient patient);
	boolean cancelAppointment(Long appointmentId, Long patientId);
    
}
