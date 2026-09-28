package com.example.plan.service;

import com.example.plan.entity.Medicine;
import com.example.plan.entity.Patient;
import com.example.plan.entity.Schedule;
import com.example.plan.exception.ResourceNotFoundException;
import com.example.plan.repository.MedicineRepository;
import com.example.plan.repository.PatientRepository;
import com.example.plan.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;

    public Schedule createSchedule(Schedule schedule) {
        if (schedule.getPatient() == null || schedule.getPatient().getId() == null) {
            throw new IllegalArgumentException("Patient ID must be provided in schedule");
        }
        if (schedule.getMedicine() == null || schedule.getMedicine().getId() == null) {
            throw new IllegalArgumentException("Medicine ID must be provided in schedule");
        }

        Long patientId = schedule.getPatient().getId();
        Long medicineId = schedule.getMedicine().getId();

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with id: " + medicineId));

        schedule.setPatient(patient);
        schedule.setMedicine(medicine);

        return scheduleRepository.save(schedule);
    }

    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public Schedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
    }

    public List<Schedule> getSchedulesByPatientId(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with id: " + patientId);
        }
        return scheduleRepository.findByPatientId(patientId);
    }

    public void deleteSchedule(Long id) {
        Schedule existing = getScheduleById(id);
        scheduleRepository.delete(existing);
    }
}
