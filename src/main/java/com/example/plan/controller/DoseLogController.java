package com.example.plan.controller;

import com.example.plan.dto.DashboardSummaryResponse;
import com.example.plan.dto.OverdueAlertResponse;
import com.example.plan.dto.TodayDoseResponse;
import com.example.plan.entity.DoseLog;
import com.example.plan.service.DoseLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/doselogs")
@RequiredArgsConstructor
public class DoseLogController {

    private final DoseLogService doseLogService;

    @PostMapping
    public ResponseEntity<DoseLog> createDoseLog(@Valid @RequestBody DoseLog doseLog) {
        DoseLog created = doseLogService.logDose(doseLog);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DoseLog>> getAllDoseLogs() {
        return ResponseEntity.ok(doseLogService.getAllDoseLogs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoseLog> getDoseLogById(@PathVariable Long id) {
        return ResponseEntity.ok(doseLogService.getDoseLogById(id));
    }

    @GetMapping("/today")
    public ResponseEntity<List<TodayDoseResponse>> getTodayExpectedDoses(
            @RequestParam Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(doseLogService.getTodayExpectedDoses(patientId, date));
    }

    @GetMapping("/today/{patientId}")
    public ResponseEntity<List<TodayDoseResponse>> getTodayExpectedDosesByPath(
            @PathVariable Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(doseLogService.getTodayExpectedDoses(patientId, date));
    }

    @GetMapping("/missed")
    public ResponseEntity<List<DoseLog>> getMissedDoseHistory(
            @RequestParam Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(doseLogService.getMissedDoseHistory(patientId, startDate, endDate));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<OverdueAlertResponse>> getOverdueAlerts(@RequestParam Long patientId) {
        return ResponseEntity.ok(doseLogService.getOverdueAlerts(patientId));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(@RequestParam Long patientId) {
        return ResponseEntity.ok(doseLogService.getDashboardSummary(patientId));
    }
}
