package com.example.plan.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Patient name cannot be blank")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Age is required")
    @Positive(message = "Age must be a positive number")
    @Column(nullable = false)
    private Integer age;

    @NotBlank(message = "Phone number cannot be blank")
    @Column(nullable = false)
    private String phone;
}
