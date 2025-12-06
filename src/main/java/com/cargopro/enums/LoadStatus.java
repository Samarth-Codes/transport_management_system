package com.cargopro.enums;

/**
 * Enum representing the status of a Load
 * - POSTED: Load is available for bidding
 * - BOOKED: Load is fully booked (remainingTrucks = 0)
 * - CANCELLED: Load has been cancelled
 */
public enum LoadStatus {
    POSTED,
    OPEN_FOR_BIDS,
    BOOKED,
    CANCELLED
}
