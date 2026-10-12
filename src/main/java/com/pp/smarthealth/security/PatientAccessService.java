package com.pp.smarthealth.security;

import com.pp.smarthealth.model.Doctor;
import com.pp.smarthealth.model.Patient;
import com.pp.smarthealth.repository.DoctorRepository;
import com.pp.smarthealth.repository.PatientRepository;
import com.pp.smarthealth.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class PatientAccessService {
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    public PatientAccessService(PatientRepository patients, DoctorRepository doctors) {
        this.patients = patients;
        this.doctors = doctors;
    }
    public boolean hasRole(Authentication auth, String role) {
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }
    public Patient currentPatient(Authentication auth) {
        if (!hasRole(auth, "PATIENT")) { throw denied(); }
        try { return patients.findByUsername(auth.getName()).orElseThrow(PatientAccessService::denied); }
        catch (org.springframework.dao.DataAccessException ex) { throw new com.pp.smarthealth.exception.TimelineStorageException(); }
    }
    public Doctor currentDoctor(Authentication auth) {
        if (!hasRole(auth, "DOCTOR")) { throw denied(); }
        try { return doctors.findByUsername(auth.getName()).orElseThrow(PatientAccessService::denied); }
        catch (org.springframework.dao.DataAccessException ex) { throw new com.pp.smarthealth.exception.TimelineStorageException(); }
    }
    public Patient doctorPatient(Long patientId, Authentication auth) {
        currentDoctor(auth);
        if (patientId == null || patientId <= 0) {
            throw new com.pp.smarthealth.exception.BadRequestException("Patient ID must be positive.");
        }
        // Matches the existing role-wide doctor access policy; no assignment relation exists.
        try { return patients.findById(patientId).orElseThrow(() -> new ResourceNotFoundException("Patient not found")); }
        catch (org.springframework.dao.DataAccessException ex) { throw new com.pp.smarthealth.exception.TimelineStorageException(); }
    }
    private static AccessDeniedException denied() {
        return new AccessDeniedException("The authenticated account is not authorized for this operation.");
    }
}
