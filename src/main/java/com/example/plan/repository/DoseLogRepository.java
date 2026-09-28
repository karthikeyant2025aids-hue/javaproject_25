package com.example.plan.repository;

import com.example.plan.entity.DoseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoseLogRepository extends JpaRepository<DoseLog, Long> {
    Optional<DoseLog> findByScheduleIdAndDate(Long scheduleId, LocalDate date);

    List<DoseLog> findBySchedulePatientIdAndDate(Long patientId, LocalDate date);

    List<DoseLog> findBySchedulePatientIdAndDateBetween(Long patientId, LocalDate startDate, LocalDate endDate);

    List<DoseLog> findBySchedulePatientIdAndTakenFalseAndDateBetween(Long patientId, LocalDate startDate, LocalDate endDate);
}
