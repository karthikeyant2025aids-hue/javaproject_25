package com.example.plan.service;

import com.example.plan.dto.DashboardSummaryResponse;
import com.example.plan.dto.DoseStatus;
import com.example.plan.dto.OverdueAlertResponse;
import com.example.plan.dto.TodayDoseResponse;
import com.example.plan.entity.DoseLog;
import com.example.plan.entity.Patient;
import com.example.plan.entity.Schedule;
import com.example.plan.exception.DoseAlreadyLoggedException;
import com.example.plan.exception.ResourceNotFoundException;
import com.example.plan.repository.DoseLogRepository;
import com.example.plan.repository.PatientRepository;
import com.example.plan.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoseLogService {

    private final DoseLogRepository doseLogRepository;
    private final ScheduleRepository scheduleRepository;
    private final PatientRepository patientRepository;

    public DoseLog logDose(DoseLog doseLog) {
        if (doseLog.getSchedule() == null || doseLog.getSchedule().getId() == null) {
            throw new IllegalArgumentException("Schedule reference with valid ID is required");
        }
        if (doseLog.getDate() == null) {
            doseLog.setDate(LocalDate.now());
        }
        if (doseLog.getTaken() == null) {
            throw new IllegalArgumentException("Taken status (true/false) must be specified");
        }

        Long scheduleId = doseLog.getSchedule().getId();
        LocalDate date = doseLog.getDate();

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        doseLog.setSchedule(schedule);

        // Business Rule: Check if a dose for this slot (scheduleId + date) is already logged
        Optional<DoseLog> existingLogOpt = doseLogRepository.findByScheduleIdAndDate(scheduleId, date);
        if (existingLogOpt.isPresent()) {
            DoseLog existingLog = existingLogOpt.get();
            if (Boolean.TRUE.equals(existingLog.getTaken())) {
                throw new DoseAlreadyLoggedException(
                        "Dose for schedule ID " + scheduleId + " (" + schedule.getMedicine().getMedicineName()
                                + ") on date " + date + " is already marked as TAKEN and cannot be logged again."
                );
            }
            // If previously marked as false (missed) and caregiver is now marking as taken or updating
            existingLog.setTaken(doseLog.getTaken());
            existingLog.setLoggedAt(LocalDateTime.now());
            return doseLogRepository.save(existingLog);
        }

        if (doseLog.getLoggedAt() == null) {
            doseLog.setLoggedAt(LocalDateTime.now());
        }
        return doseLogRepository.save(doseLog);
    }

    public List<DoseLog> getAllDoseLogs() {
        return doseLogRepository.findAll();
    }

    public DoseLog getDoseLogById(Long id) {
        return doseLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DoseLog not found with id: " + id));
    }

    public List<TodayDoseResponse> getTodayExpectedDoses(Long patientId, LocalDate date) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        boolean isToday = queryDate.equals(LocalDate.now());

        List<Schedule> schedules = scheduleRepository.findByPatientId(patientId);
        List<DoseLog> logs = doseLogRepository.findBySchedulePatientIdAndDate(patientId, queryDate);

        Map<Long, DoseLog> logMap = logs.stream()
                .collect(Collectors.toMap(l -> l.getSchedule().getId(), Function.identity(), (a, b) -> a));

        List<TodayDoseResponse> responses = new ArrayList<>();

        for (Schedule s : schedules) {
            DoseLog log = logMap.get(s.getId());
            DoseStatus status;
            boolean isOverdue = false;
            Long logId = null;
            Boolean taken = null;

            if (log != null) {
                logId = log.getId();
                taken = log.getTaken();
                status = Boolean.TRUE.equals(taken) ? DoseStatus.TAKEN : DoseStatus.MISSED;
            } else {
                if (isToday) {
                    if (currentTime.isAfter(s.getDoseTime().plusHours(1))) {
                        status = DoseStatus.OVERDUE;
                        isOverdue = true;
                    } else if (currentTime.isAfter(s.getDoseTime())) {
                        status = DoseStatus.PENDING;
                    } else {
                        status = DoseStatus.PENDING;
                    }
                } else if (queryDate.isBefore(LocalDate.now())) {
                    status = DoseStatus.MISSED;
                } else {
                    status = DoseStatus.PENDING;
                }
            }

            TodayDoseResponse resp = TodayDoseResponse.builder()
                    .scheduleId(s.getId())
                    .patientId(patient.getId())
                    .patientName(patient.getName())
                    .medicineId(s.getMedicine().getId())
                    .medicineName(s.getMedicine().getMedicineName())
                    .dosage(s.getMedicine().getDosage())
                    .frequency(s.getMedicine().getFrequency())
                    .doseTime(s.getDoseTime())
                    .date(queryDate)
                    .status(status)
                    .doseLogId(logId)
                    .taken(taken)
                    .overdue(isOverdue)
                    .build();

            responses.add(resp);
        }

        return responses;
    }

    public List<DoseLog> getMissedDoseHistory(Long patientId, LocalDate startDate, LocalDate endDate) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with id: " + patientId);
        }
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        return doseLogRepository.findBySchedulePatientIdAndTakenFalseAndDateBetween(patientId, startDate, endDate);
    }

    public List<OverdueAlertResponse> getOverdueAlerts(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with id: " + patientId);
        }

        List<Schedule> schedules = scheduleRepository.findByPatientId(patientId);
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        List<DoseLog> todayLogs = doseLogRepository.findBySchedulePatientIdAndDate(patientId, today);
        Map<Long, DoseLog> logMap = todayLogs.stream()
                .collect(Collectors.toMap(l -> l.getSchedule().getId(), Function.identity(), (a, b) -> a));

        List<OverdueAlertResponse> alerts = new ArrayList<>();

        for (Schedule schedule : schedules) {
            DoseLog log = logMap.get(schedule.getId());
            // Overdue condition: Dose not marked TAKEN AND current time is > scheduled dose time + 1 hour
            boolean taken = (log != null && Boolean.TRUE.equals(log.getTaken()));

            if (!taken && currentTime.isAfter(schedule.getDoseTime().plusHours(1))) {
                long minutesOverdue = Duration.between(schedule.getDoseTime(), currentTime).toMinutes();

                OverdueAlertResponse alert = OverdueAlertResponse.builder()
                        .scheduleId(schedule.getId())
                        .patientId(schedule.getPatient().getId())
                        .patientName(schedule.getPatient().getName())
                        .medicineName(schedule.getMedicine().getMedicineName())
                        .dosage(schedule.getMedicine().getDosage())
                        .doseTime(schedule.getDoseTime())
                        .date(today)
                        .minutesOverdue(minutesOverdue)
                        .alertMessage("ALERT: Dose of " + schedule.getMedicine().getMedicineName() + " ("
                                + schedule.getMedicine().getDosage() + ") for " + schedule.getPatient().getName()
                                + " scheduled at " + schedule.getDoseTime() + " is overdue by " + minutesOverdue + " minutes!")
                        .build();

                alerts.add(alert);
            }
        }

        return alerts;
    }

    public DashboardSummaryResponse getDashboardSummary(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        List<TodayDoseResponse> todayDoses = getTodayExpectedDoses(patientId, LocalDate.now());

        int totalScheduled = todayDoses.size();
        int takenCount = (int) todayDoses.stream().filter(d -> DoseStatus.TAKEN.equals(d.getStatus())).count();
        int missedCount = (int) todayDoses.stream().filter(d -> DoseStatus.MISSED.equals(d.getStatus())).count();
        int overdueCount = (int) todayDoses.stream().filter(d -> DoseStatus.OVERDUE.equals(d.getStatus()) || Boolean.TRUE.equals(d.getOverdue())).count();
        int pendingCount = (int) todayDoses.stream().filter(d -> DoseStatus.PENDING.equals(d.getStatus())).count();

        return DashboardSummaryResponse.builder()
                .patientId(patient.getId())
                .patientName(patient.getName())
                .date(LocalDate.now())
                .totalScheduled(totalScheduled)
                .takenCount(takenCount)
                .missedCount(missedCount)
                .overdueCount(overdueCount)
                .pendingCount(pendingCount)
                .build();
    }
}
