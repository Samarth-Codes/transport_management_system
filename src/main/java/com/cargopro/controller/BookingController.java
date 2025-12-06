package com.cargopro.controller;

import com.cargopro.dto.BookingResponse;
import com.cargopro.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/booking")
@Tag(name = "Booking Management", description = "APIs for managing bookings (accepted bids)")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    @Operation(summary = "Accept a bid", description = "Accepts a bid and creates a booking. Reduces trucks on load and transporter.")
    public ResponseEntity<BookingResponse> acceptBid(@RequestParam UUID bidId) {
        BookingResponse response = bookingService.acceptBid(bidId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking details", description = "Get full details of a booking")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable UUID id) {
        BookingResponse booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking", description = "Cancel a booking and restore capacity")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable UUID id) {
        BookingResponse response = bookingService.cancelBooking(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/load/{loadId}")
    @Operation(summary = "Get bookings for a load", description = "Retrieves all bookings for a specific load")
    public ResponseEntity<List<BookingResponse>> getBookingsByLoadId(@PathVariable UUID loadId) {
        List<BookingResponse> bookings = bookingService.getBookingsByLoadId(loadId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/transporter/{transporterId}")
    @Operation(summary = "Get bookings for a transporter", description = "Retrieves all bookings for a specific transporter")
    public ResponseEntity<List<BookingResponse>> getBookingsByTransporterId(
            @PathVariable UUID transporterId) {
        List<BookingResponse> bookings = bookingService.getBookingsByTransporterId(transporterId);
        return ResponseEntity.ok(bookings);
    }

}
