package com.example.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {
    private Long patientId;
    private String patientName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private int totalScheduled;
    private int takenCount;
    private int missedCount;
    private int overdueCount;
    private int pendingCount;
}
