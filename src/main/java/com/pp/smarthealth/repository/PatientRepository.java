package com.pp.smarthealth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pp.smarthealth.model.Patient;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
	    List<Patient> findByNameContainingIgnoreCase(String name);
	    List<Patient> findByAge(int age);
	    List<Patient> findByMedicalConditionContainingIgnoreCase(String medicalCondition);
	    Optional<Patient> findByUsername(String username);
}