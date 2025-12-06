package com.cargopro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.util.UUID;

@Data
public class BidRequest {

    @NotNull(message = "Transporter ID is required")
    private UUID transporterId;

    @NotNull(message = "Load ID is required")
    private UUID loadId;

    @NotNull(message = "Proposed rate is required")
    @Positive(message = "Proposed rate must be positive")
    private Double proposedRate;

    @NotNull(message = "Trucks offered is required")
    @Positive(message = "Trucks offered must be positive")
    private Integer trucksOffered;
}
