package com.example.plan.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dose_logs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"schedule_id", "date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate date;

    @NotNull(message = "Taken status is required")
    @Column(nullable = false)
    private Boolean taken;

    @NotNull(message = "Schedule reference is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    private LocalDateTime loggedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        if (this.loggedAt == null) {
            this.loggedAt = LocalDateTime.now();
        }
    }
}
