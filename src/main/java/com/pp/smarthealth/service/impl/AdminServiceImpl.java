package com.pp.smarthealth.service.impl;

import com.pp.smarthealth.exception.ResourceNotFoundException;
import com.pp.smarthealth.model.Admin;
import com.pp.smarthealth.repository.AdminRepository;
import com.pp.smarthealth.service.AdminService;
import com.pp.smarthealth.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private JwtUtil jwtService;

    @Autowired
    AuthenticationManager authManager;
    
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
     
    @Override
    public Admin register(Admin admin) {
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        adminRepository.save(admin);
        return admin;
    }
    @Override
    public String verify(Admin admin) {
        Authentication authentication = authManager.authenticate(new UsernamePasswordAuthenticationToken(admin.getUsername(), admin.getPassword()));
        if (authentication.isAuthenticated()) {
            return jwtService.generateToken(admin.getUsername());
        } else {
            return "fail";
        }
    }
	@Override
	public void deleteUser(Long id) {
	  adminRepository.deleteById(id);
		
	}
	@Override
	public List<Admin> getAllUsers() {
		return adminRepository.findAll();
	}
	
    
  
}
