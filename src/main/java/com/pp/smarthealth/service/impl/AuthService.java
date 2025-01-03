package com.pp.smarthealth.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.pp.smarthealth.exception.ResourceNotFoundException;
import com.pp.smarthealth.payload.request.AuthRequest;
import com.pp.smarthealth.repository.AdminRepository;
import com.pp.smarthealth.repository.DoctorRepository;
import com.pp.smarthealth.repository.PatientRepository;
import com.pp.smarthealth.util.JwtUtil;

@Service
public class AuthService {
	  @Autowired
	    private PatientRepository patientRepository;

	    @Autowired
	    private DoctorRepository doctorRepository;

	    @Autowired
	    private AdminRepository adminRepository;

	    @Autowired
	    private JwtUtil jwtService;

	    @Autowired
	    AuthenticationManager authManager;

	        private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

	       
	        public String verify(AuthRequest authRequest) {
	            return authenticateUser(authRequest.getUsername(),authRequest.getPassword());
	        }

	        public String authenticateUser(String username, String password) {
	            Authentication authentication = authManager.authenticate(
	                    new UsernamePasswordAuthenticationToken(username, password)
	            );
	            if (authentication.isAuthenticated()) {
	                return jwtService.generateToken(username);
	            } else {
	                throw new RuntimeException("Authentication failed for username: " + username);
	            }
	        }
	    }


