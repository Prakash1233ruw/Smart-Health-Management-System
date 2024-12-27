package com.pp.smarthealth.service.impl;

import com.pp.smarthealth.dto.PatientDTO;
import com.pp.smarthealth.exception.ResourceNotFoundException;
import com.pp.smarthealth.model.Patient;
import com.pp.smarthealth.repository.PatientRepository;
import com.pp.smarthealth.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientServiceImpl implements PatientService {

	@Autowired
	private PatientRepository patientRepository;

	@Override
	public PatientDTO savePatient(Patient patient) {
		Patient savedPatient = patientRepository.save(patient);
		return convertToDTO(savedPatient);
	}

	@Override
	public PatientDTO updatePatient(Long id, Patient patientDetails) {
		Patient patient = patientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
		patient.setName(patientDetails.getName());
		patient.setAge(patientDetails.getAge());
		patient.setMedicalCondition(patientDetails.getMedicalCondition());
		Patient updatedPatient = patientRepository.save(patient);
		return convertToDTO(updatedPatient);
	}

	@Override
	public void deletePatient(Long id) {
		Patient patient = patientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
		patientRepository.delete(patient);
	}

	@Override
	public PatientDTO getPatientById(Long id) {
		Patient patient = patientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
		return convertToDTO(patient);
	}

	@Override
	public List<PatientDTO> getAllPatients() {
		List<Patient> patients = patientRepository.findAll();
		return patients.stream().map(this::convertToDTO).collect(Collectors.toList());
	}

	private PatientDTO convertToDTO(Patient patient) {
		PatientDTO patientDTO = new PatientDTO();
		patientDTO.setId(patient.getId());
		patientDTO.setName(patient.getName());
		patientDTO.setAge(patient.getAge());
		patientDTO.setMedicalCondition(patient.getMedicalCondition());
		return patientDTO;
	}

	@Override
	public List<PatientDTO> searchPatientsWithName(String name) {
		PatientDTO patientDTO = new PatientDTO();
		List<PatientDTO> patientDTOs=new ArrayList<>();

		List<Patient> patient = patientRepository.findByNameContainingIgnoreCase(name);
		for(Patient patientTemp:patient) {
			patientDTO.setId(patientTemp.getId());
			patientDTO.setName(patientTemp.getName());
			patientDTO.setAge(patientTemp.getAge());
			patientDTO.setMedicalCondition(patientTemp.getMedicalCondition());
			patientDTOs.add(patientDTO);
		}
		return patientDTOs;
	}

	@Override
	public List<PatientDTO> searchPatientsWithAge(int age) {

		PatientDTO patientDTO = new PatientDTO();

		List<PatientDTO> patientDTOs=new ArrayList<>();

		List<Patient> patient = patientRepository.findByAge(age);
		
		for(Patient patientTemp:patient) {
			patientDTO.setId(patientTemp.getId());
			patientDTO.setName(patientTemp.getName());
			patientDTO.setAge(patientTemp.getAge());
			patientDTO.setMedicalCondition(patientTemp.getMedicalCondition());
			patientDTOs.add(patientDTO);
		}
		return patientDTOs;
	}

	@Override
	public List<PatientDTO> searchPatientsMedicalCondition(String medicalCondition) {
		PatientDTO patientDTO = new PatientDTO();

		List<PatientDTO> patientDTOs=new ArrayList<>();

		List<Patient> patient = patientRepository.findByMedicalConditionContainingIgnoreCase(medicalCondition);
		for(Patient patientTemp:patient) {
			patientDTO.setId(patientTemp.getId());
			patientDTO.setName(patientTemp.getName());
			patientDTO.setAge(patientTemp.getAge());
			patientDTO.setMedicalCondition(patientTemp.getMedicalCondition());
			patientDTOs.add(patientDTO);
		}
		return patientDTOs;

	}

	@Override
	public List<PatientDTO> searchPatients(String name, Integer age, String medicalCondition) {
		// TODO Auto-generated method stub
		return null;
	}
}
