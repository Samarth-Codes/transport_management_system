package com.cargopro.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Home Controller
 * Handles root URL requests and provides API information
 */
@RestController
public class HomeController {

    /**
     * Root endpoint - redirects to API info
     * GET /
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Transport Management System API");
        response.put("version", "1.0.0");
        response.put("swagger-ui", "http://localhost:8080/swagger-ui/index.html");
        response.put("api-docs", "http://localhost:8080/v3/api-docs");
        response.put("endpoints", Map.of(
                "loads", "/load",
                "transporters", "/transporter",
                "bids", "/bid",
                "bookings", "/booking"));
        return ResponseEntity.ok(response);
    }
}
