package com.example.plan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class CarePlanIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Complete CarePlan Workflow: Patient -> Medicine -> Schedule -> DoseLog")
    void testCompleteWorkflow() throws Exception {
        // 1. Create Patient
        String patientJson = """
                {
                    "name": "Ravi",
                    "age": 70,
                    "phone": "9876543210"
                }
                """;

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name", is("Ravi")))
                .andExpect(jsonPath("$.age", is(70)));

        // 2. Create Medicine
        String medicineJson = """
                {
                    "medicineName": "Paracetamol",
                    "dosage": "500mg",
                    "frequency": "Twice Daily"
                }
                """;

        mockMvc.perform(post("/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(medicineJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.medicineName", is("Paracetamol")));

        // 3. Create Schedule
        String scheduleJson = """
                {
                    "doseTime": "08:00:00",
                    "patient": { "id": 1 },
                    "medicine": { "id": 1 }
                }
                """;

        mockMvc.perform(post("/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.doseTime", is("08:00:00")));

        // 4. Log Dose as Taken
        String doseLogJson = """
                {
                    "date": "2026-09-28",
                    "taken": true,
                    "schedule": { "id": 1 }
                }
                """;

        mockMvc.perform(post("/doselogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doseLogJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.taken", is(true)));

        // 5. Enforce Business Rule: Attempting to log the SAME dose slot again when TAKEN should fail with clear message
        mockMvc.perform(post("/doselogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doseLogJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Bad Request - Business Rule Violated")))
                .andExpect(jsonPath("$.message", containsString("already marked as TAKEN")));

        // 6. Check Today's Expected Doses
        mockMvc.perform(get("/doselogs/today")
                        .param("patientId", "1")
                        .param("date", "2026-09-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("TAKEN")));

        // 7. Check Dashboard Summary
        mockMvc.perform(get("/doselogs/dashboard")
                        .param("patientId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScheduled", is(1)))
                .andExpect(jsonPath("$.takenCount", is(1)));
    }

    @Test
    @DisplayName("Validation Error Handling: Invalid patient input")
    void testInvalidPatientInput() throws Exception {
        String invalidPatientJson = """
                {
                    "name": "",
                    "age": -5,
                    "phone": ""
                }
                """;

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPatientJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.details", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Resource Not Found Handling: Non-existent Patient")
    void testResourceNotFound() throws Exception {
        mockMvc.perform(get("/patients/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("Patient not found with id: 99999")));
    }
}
