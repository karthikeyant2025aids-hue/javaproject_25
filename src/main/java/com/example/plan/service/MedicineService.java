package com.example.plan.service;

import com.example.plan.entity.Medicine;
import com.example.plan.exception.ResourceNotFoundException;
import com.example.plan.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;

    public Medicine createMedicine(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    public Medicine getMedicineById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with id: " + id));
    }

    public Medicine updateMedicine(Long id, Medicine details) {
        Medicine existing = getMedicineById(id);
        existing.setMedicineName(details.getMedicineName());
        existing.setDosage(details.getDosage());
        existing.setFrequency(details.getFrequency());
        return medicineRepository.save(existing);
    }

    public void deleteMedicine(Long id) {
        Medicine existing = getMedicineById(id);
        medicineRepository.delete(existing);
    }
}
