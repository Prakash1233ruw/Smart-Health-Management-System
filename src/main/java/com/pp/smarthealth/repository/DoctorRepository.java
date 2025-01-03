package com.pp.smarthealth.repository;

import com.pp.smarthealth.model.Admin;
import com.pp.smarthealth.model.Doctor;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
	Optional<Doctor> findByUsername(String username);
}
