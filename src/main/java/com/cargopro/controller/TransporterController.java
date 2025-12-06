package com.cargopro.controller;

import com.cargopro.dto.TransporterRequest;
import com.cargopro.dto.TransporterResponse;
import com.cargopro.entity.TruckAvailability;
import com.cargopro.service.TransporterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transporter")
@Tag(name = "Transporter Management", description = "APIs for managing transporters")
public class TransporterController {

    @Autowired
    private TransporterService transporterService;

    @PostMapping
    @Operation(summary = "Create a transporter", description = "Registers a new transporter/company")
    public ResponseEntity<TransporterResponse> createTransporter(
            @Valid @RequestBody TransporterRequest request) {
        TransporterResponse response = transporterService.createTransporter(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all transporters", description = "Retrieves a list of all transporters")
    public ResponseEntity<List<TransporterResponse>> getAllTransporters() {
        List<TransporterResponse> transporters = transporterService.getAllTransporters();
        return ResponseEntity.ok(transporters);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transporter by ID", description = "Retrieves a specific transporter by its ID")
    public ResponseEntity<TransporterResponse> getTransporterById(@PathVariable UUID id) {
        TransporterResponse transporter = transporterService.getTransporterById(id);
        return ResponseEntity.ok(transporter);
    }

    @PutMapping("/{id}/trucks")
    @Operation(summary = "Update available trucks", description = "Updates the available trucks for a transporter")
    public ResponseEntity<TransporterResponse> updateAvailableTrucks(
            @PathVariable UUID id,
            @RequestBody List<TruckAvailability> trucks) {
        TransporterResponse response = transporterService.updateAvailableTrucks(id, trucks);
        return ResponseEntity.ok(response);
    }
}
