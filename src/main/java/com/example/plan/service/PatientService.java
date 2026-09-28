package com.example.plan.service;

import com.example.plan.entity.Patient;
import com.example.plan.exception.ResourceNotFoundException;
import com.example.plan.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public Patient createPatient(Patient patient) {
        return patientRepository.save(patient);
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Patient getPatientById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }

    public Patient updatePatient(Long id, Patient details) {
        Patient existing = getPatientById(id);
        existing.setName(details.getName());
        existing.setAge(details.getAge());
        existing.setPhone(details.getPhone());
        return patientRepository.save(existing);
    }

    public void deletePatient(Long id) {
        Patient existing = getPatientById(id);
        patientRepository.delete(existing);
    }
}
