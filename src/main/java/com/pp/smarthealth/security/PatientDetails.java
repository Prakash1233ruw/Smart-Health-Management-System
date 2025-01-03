package com.pp.smarthealth.security;

import com.pp.smarthealth.model.Patient;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class PatientDetails implements UserDetails {

    private final Patient patient;

    public PatientDetails(Patient patient) {
        this.patient = patient;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
   
        return Collections.singletonList(() -> "ROLE_PATIENT");
    }

    @Override
    public String getPassword() {
        return patient.getPassword();
    }

    @Override
    public String getUsername() {
        return patient.getUsername(); 
    }

    public String getMedicalCondition() {
        return patient.getMedicalCondition(); 
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
