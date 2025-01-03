package com.pp.smarthealth.service.impl;

import com.pp.smarthealth.model.Admin;
import com.pp.smarthealth.model.Doctor;
import com.pp.smarthealth.model.Patient;
import com.pp.smarthealth.repository.AdminRepository;
import com.pp.smarthealth.repository.DoctorRepository;
import com.pp.smarthealth.repository.PatientRepository;
import com.pp.smarthealth.security.AdminDetails;
import com.pp.smarthealth.security.DoctorDetails;
import com.pp.smarthealth.security.PatientDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
      
        Admin admin = adminRepository.findByUsername(username).orElse(null);
        if (admin != null) {
            return new AdminDetails(admin);
        }

       
        Doctor doctor = doctorRepository.findByUsername(username).orElse(null);
        if (doctor != null) {
            return new DoctorDetails(doctor);
        }

        
        Patient patient = patientRepository.findByUsername(username).orElse(null);
        if (patient != null) {
            return new PatientDetails(patient);
        }

       
        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}
