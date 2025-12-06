package com.cargopro.integration;

import com.cargopro.dto.TransporterRequest;
import com.cargopro.entity.TruckAvailability;
import com.cargopro.enums.TruckType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Transporter Management
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TransporterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldCreateTransporterSuccessfully() throws Exception {
        TransporterRequest request = new TransporterRequest();
        request.setCompanyName("Fast Logistics");
        request.setRating(4.5);
        request.setAvailableTrucks(Arrays.asList(
                new TruckAvailability(TruckType.LARGE.name(), 20)));

        mockMvc.perform(post("/transporter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transporterId").exists())
                .andExpect(jsonPath("$.companyName", is("Fast Logistics")))
                .andExpect(jsonPath("$.rating", is(4.5)));
        // Verifying available trucks structure might be complex via jsonPath simply,
        // but checking size or basic property is okay.
    }
}
