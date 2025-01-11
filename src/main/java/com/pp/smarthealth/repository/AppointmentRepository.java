package com.pp.smarthealth.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pp.smarthealth.model.Appointment;
import com.pp.smarthealth.model.Patient;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDoctorIdAndDateTimeAfterOrderByDateTimeAsc(Long doctorId, LocalDateTime dateTime);

	 @Query("SELECT a FROM Appointment a WHERE a.dateTime BETWEEN :start AND :end")
	    List<Appointment> findAppointmentsForNextDay(LocalDateTime start, LocalDateTime end);
	 
	 List<Appointment> findByPatient(Patient patient);
	 
	 @Modifying
	 @Query("DELETE FROM Appointment a WHERE a.patient.id = :patientId")
	 void deleteByPatientId(@Param("patientId") Long patientId);

}