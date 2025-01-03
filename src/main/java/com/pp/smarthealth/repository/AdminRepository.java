package com.pp.smarthealth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pp.smarthealth.model.Admin;



	@Repository
	public interface AdminRepository extends JpaRepository<Admin, Long> {

		   Optional<Admin> findByUsername(String username);
	}
