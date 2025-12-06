package com.cargopro.dto;

import com.cargopro.entity.TruckAvailability;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.util.List;

/**
 * DTO for creating a new Transporter
 */
@Data
public class TransporterRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotNull(message = "Rating is required")
    @Positive(message = "Rating must be positive")
    private Double rating; // 0.0 to 5.0

    @NotNull(message = "Available trucks list is required")
    private List<TruckAvailability> availableTrucks;
}
