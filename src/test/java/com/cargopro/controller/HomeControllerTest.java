package com.cargopro.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void home_ShouldReturnApiInfo() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transport Management System API"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.swagger-ui").exists())
                .andExpect(jsonPath("$.api-docs").exists())
                .andExpect(jsonPath("$.endpoints").exists())
                .andExpect(jsonPath("$.endpoints.loads").value("/load"))
                .andExpect(jsonPath("$.endpoints.transporters").value("/transporter"))
                .andExpect(jsonPath("$.endpoints.bids").value("/bid"))
                .andExpect(jsonPath("$.endpoints.bookings").value("/booking"));
    }
}
