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
public class OverdueAlertResponse {
    private Long scheduleId;
    private Long patientId;
    private String patientName;
    private String medicineName;
    private String dosage;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime doseTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String alertMessage;
    private long minutesOverdue;
}
