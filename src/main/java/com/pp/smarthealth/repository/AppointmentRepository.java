package com.pp.smarthealth.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pp.smarthealth.model.Appointment;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
//	 @Query("SELECT a FROM Appointment a WHERE " +
//	           "a.patient.name LIKE %:keyword% OR " +
//	           "a.patient.age = :keywordInt OR " +
//	           "a.patient.medicalCondition LIKE %:keyword%")
//	          // + "OR " +
//	        //   "a.patient.email LIKE %:keyword%")
//	    List<Appointment> findByPatientDetails(@Param("keyword") String keyword, @Param("keywordInt") int keywordInt);

	 @Query("SELECT a FROM Appointment a WHERE a.dateTime BETWEEN :start AND :end")
	    List<Appointment> findAppointmentsForNextDay(LocalDateTime start, LocalDateTime end);
}