package com.example.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodayDoseResponse {
    private Long scheduleId;
    private Long patientId;
    private String patientName;
    private Long medicineId;
    private String medicineName;
    private String dosage;
    private String frequency;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime doseTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private DoseStatus status;
    private Long doseLogId;
    private Boolean taken;
    private Boolean overdue;
}
