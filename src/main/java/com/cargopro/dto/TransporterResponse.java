package com.cargopro.dto;

import com.cargopro.entity.TruckAvailability;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

/**
 * DTO for returning Transporter information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransporterResponse {
    private UUID transporterId;
    private String companyName;
    private Double rating;
    private List<TruckAvailability> availableTrucks;
}
