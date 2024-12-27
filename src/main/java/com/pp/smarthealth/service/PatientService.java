package com.pp.smarthealth.service;

import com.pp.smarthealth.dto.PatientDTO;
import com.pp.smarthealth.model.Patient;

import java.util.List;

public interface PatientService {
    PatientDTO savePatient(Patient patient);
    PatientDTO getPatientById(Long id);
    List<PatientDTO> getAllPatients();
    PatientDTO updatePatient(Long id, Patient patientDetails);
    void deletePatient(Long id);
	List<PatientDTO> searchPatientsWithName(String name);
	List<PatientDTO> searchPatientsWithAge(int age);
	List<PatientDTO> searchPatientsMedicalCondition(String medicalCondition);
	List<PatientDTO> searchPatients(String name, Integer age, String medicalCondition);
}
