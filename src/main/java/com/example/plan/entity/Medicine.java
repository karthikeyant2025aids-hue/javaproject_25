package com.example.plan.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "medicines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Medicine name cannot be blank")
    @Column(nullable = false)
    private String medicineName;

    @NotBlank(message = "Dosage cannot be blank")
    @Column(nullable = false)
    private String dosage;

    @NotBlank(message = "Frequency cannot be blank")
    @Column(nullable = false)
    private String frequency;
}
