package com.cargopro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * DTO for returning best bids with scoring
 * Includes the calculated score (rate + rating) for sorting
 */
@Data
@AllArgsConstructor
public class BestBidResponse {
    private UUID bidId;
    private UUID transporterId;
    private String transporterName;
    private Double transporterRating;
    private Double proposedRate;
    private Integer trucksOffered;
    private Double score;
    private Timestamp submittedAt;
}
