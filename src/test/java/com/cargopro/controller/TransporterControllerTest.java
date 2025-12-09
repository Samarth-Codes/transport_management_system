package com.cargopro.controller;

import com.cargopro.dto.TransporterRequest;
import com.cargopro.dto.TransporterResponse;
import com.cargopro.entity.TruckAvailability;
import com.cargopro.exception.ResourceNotFoundException;
import com.cargopro.service.TransporterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransporterController.class)
class TransporterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransporterService transporterService;

    private TransporterResponse testTransporterResponse;
    private UUID transporterId;

    @BeforeEach
    void setUp() {
        transporterId = UUID.randomUUID();
        testTransporterResponse = new TransporterResponse(
                transporterId,
                "Test Logistics",
                4.5,
                Arrays.asList(
                        new TruckAvailability("LARGE", 10),
                        new TruckAvailability("MEDIUM", 5)));
    }

    @Test
    void createTransporter_Success() throws Exception {
        TransporterRequest request = new TransporterRequest();
        request.setCompanyName("Test Logistics");
        request.setRating(4.5);
        request.setAvailableTrucks(Arrays.asList(new TruckAvailability("LARGE", 10)));

        when(transporterService.createTransporter(any(TransporterRequest.class)))
                .thenReturn(testTransporterResponse);

        mockMvc.perform(post("/transporter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transporterId").value(transporterId.toString()))
                .andExpect(jsonPath("$.companyName").value("Test Logistics"))
                .andExpect(jsonPath("$.rating").value(4.5));
    }

    @Test
    void createTransporter_ValidationError_MissingCompanyName() throws Exception {
        TransporterRequest request = new TransporterRequest();
        // Missing companyName
        request.setRating(4.5);
        request.setAvailableTrucks(Arrays.asList(new TruckAvailability("LARGE", 10)));

        mockMvc.perform(post("/transporter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.companyName").value("Company name is required"));
    }

    @Test
    void createTransporter_ValidationError_MissingRating() throws Exception {
        TransporterRequest request = new TransporterRequest();
        request.setCompanyName("Test Logistics");
        // Missing rating
        request.setAvailableTrucks(Arrays.asList(new TruckAvailability("LARGE", 10)));

        mockMvc.perform(post("/transporter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.rating").value("Rating is required"));
    }

    @Test
    void createTransporter_ValidationError_NegativeRating() throws Exception {
        TransporterRequest request = new TransporterRequest();
        request.setCompanyName("Test Logistics");
        request.setRating(-1.0); // Invalid
        request.setAvailableTrucks(Arrays.asList(new TruckAvailability("LARGE", 10)));

        mockMvc.perform(post("/transporter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.rating").value("Rating must be positive"));
    }

    @Test
    void createTransporter_ValidationError_MissingAvailableTrucks() throws Exception {
        TransporterRequest request = new TransporterRequest();
        request.setCompanyName("Test Logistics");
        request.setRating(4.5);
        // Missing availableTrucks

        mockMvc.perform(post("/transporter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.availableTrucks").value("Available trucks list is required"));
    }

    @Test
    void getAllTransporters_Success() throws Exception {
        TransporterResponse transporter2 = new TransporterResponse(
                UUID.randomUUID(), "Another Logistics", 3.5, Collections.emptyList());
        when(transporterService.getAllTransporters())
                .thenReturn(Arrays.asList(testTransporterResponse, transporter2));

        mockMvc.perform(get("/transporter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].companyName").value("Test Logistics"))
                .andExpect(jsonPath("$[1].companyName").value("Another Logistics"));
    }

    @Test
    void getAllTransporters_Empty() throws Exception {
        when(transporterService.getAllTransporters()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/transporter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getTransporterById_Success() throws Exception {
        when(transporterService.getTransporterById(transporterId)).thenReturn(testTransporterResponse);

        mockMvc.perform(get("/transporter/{id}", transporterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transporterId").value(transporterId.toString()))
                .andExpect(jsonPath("$.companyName").value("Test Logistics"));
    }

    @Test
    void getTransporterById_NotFound() throws Exception {
        when(transporterService.getTransporterById(transporterId))
                .thenThrow(new ResourceNotFoundException("Transporter not found with id: " + transporterId));

        mockMvc.perform(get("/transporter/{id}", transporterId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("Transporter not found")));
    }

    @Test
    void updateAvailableTrucks_Success() throws Exception {
        List<TruckAvailability> newTrucks = Arrays.asList(
                new TruckAvailability("SMALL", 15),
                new TruckAvailability("LARGE", 25));
        TransporterResponse updatedResponse = new TransporterResponse(
                transporterId, "Test Logistics", 4.5, newTrucks);
        when(transporterService.updateAvailableTrucks(eq(transporterId), any()))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/transporter/{id}/trucks", transporterId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTrucks)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableTrucks", hasSize(2)));
    }

    @Test
    void updateAvailableTrucks_NotFound() throws Exception {
        List<TruckAvailability> newTrucks = Arrays.asList(new TruckAvailability("LARGE", 10));
        when(transporterService.updateAvailableTrucks(eq(transporterId), any()))
                .thenThrow(new ResourceNotFoundException("Transporter not found with id: " + transporterId));

        mockMvc.perform(put("/transporter/{id}/trucks", transporterId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTrucks)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("Transporter not found")));
    }
}
