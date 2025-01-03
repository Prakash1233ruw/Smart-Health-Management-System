package com.pp.smarthealth.security;

import com.pp.smarthealth.model.Doctor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class DoctorDetails implements UserDetails {

    private final Doctor doctor;

    public DoctorDetails(Doctor doctor) {
        this.doctor = doctor;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        
        return Collections.singletonList(() -> "ROLE_DOCTOR");
    }

    @Override
    public String getPassword() {
        return doctor.getPassword(); 
    }

    @Override
    public String getUsername() {
        return doctor.getUsername();
    }

    public String getSpecialization() {
        return doctor.getSpecialization();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
