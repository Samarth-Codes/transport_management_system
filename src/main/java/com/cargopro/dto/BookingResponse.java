package com.cargopro.dto;

import com.cargopro.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * DTO for returning Booking information
 */
@Data
@AllArgsConstructor
public class BookingResponse {
    private UUID bookingId;
    private UUID loadId;
    private UUID bidId;
    private UUID transporterId;
    private String transporterName;
    private Integer allocatedTrucks;
    private Double finalRate;
    private BookingStatus status;
    private Timestamp bookedAt;
}
