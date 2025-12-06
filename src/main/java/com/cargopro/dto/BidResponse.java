package com.cargopro.dto;

import com.cargopro.enums.BidStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * DTO for returning Bid information
 */
@Data
@AllArgsConstructor
public class BidResponse {
    private UUID bidId;
    private Double proposedRate;
    private Integer trucksOffered;
    private UUID loadId;
    private UUID transporterId;
    private String transporterName;
    private Double transporterRating;
    private Timestamp submittedAt;
    private BidStatus status;
}
