package com.cargopro.controller;

import com.cargopro.dto.BookingResponse;
import com.cargopro.enums.BookingStatus;
import com.cargopro.exception.InvalidStatusTransitionException;
import com.cargopro.exception.ResourceNotFoundException;
import com.cargopro.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    private BookingResponse testBookingResponse;
    private UUID bookingId;
    private UUID loadId;
    private UUID bidId;
    private UUID transporterId;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        loadId = UUID.randomUUID();
        bidId = UUID.randomUUID();
        transporterId = UUID.randomUUID();
        testBookingResponse = new BookingResponse(
                bookingId,
                loadId,
                bidId,
                transporterId,
                "Test Company",
                5,
                500.0,
                BookingStatus.CONFIRMED,
                new Timestamp(System.currentTimeMillis()));
    }

    @Test
    void acceptBid_Success() throws Exception {
        when(bookingService.acceptBid(bidId)).thenReturn(testBookingResponse);

        mockMvc.perform(post("/booking")
                .param("bidId", bidId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void acceptBid_BidNotFound() throws Exception {
        when(bookingService.acceptBid(bidId))
                .thenThrow(new ResourceNotFoundException("Bid not found with id: " + bidId));

        mockMvc.perform(post("/booking")
                .param("bidId", bidId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("Bid not found")));
    }

    @Test
    void acceptBid_InvalidStatus() throws Exception {
        when(bookingService.acceptBid(bidId))
                .thenThrow(new InvalidStatusTransitionException("Cannot accept bid for a cancelled load"));

        mockMvc.perform(post("/booking")
                .param("bidId", bidId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot accept bid for a cancelled load"));
    }

    @Test
    void acceptBid_LoadFullyBooked() throws Exception {
        when(bookingService.acceptBid(bidId))
                .thenThrow(new InvalidStatusTransitionException("Load is already fully booked"));

        mockMvc.perform(post("/booking")
                .param("bidId", bidId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Load is already fully booked"));
    }

    @Test
    void getBookingById_Success() throws Exception {
        when(bookingService.getBookingById(bookingId)).thenReturn(testBookingResponse);

        mockMvc.perform(get("/booking/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.allocatedTrucks").value(5));
    }

    @Test
    void getBookingById_NotFound() throws Exception {
        when(bookingService.getBookingById(bookingId))
                .thenThrow(new ResourceNotFoundException("Booking not found: " + bookingId));

        mockMvc.perform(get("/booking/{id}", bookingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("Booking not found")));
    }

    @Test
    void cancelBooking_Success() throws Exception {
        BookingResponse cancelledResponse = new BookingResponse(
                bookingId, loadId, bidId, transporterId, "Test Company",
                5, 500.0, BookingStatus.CANCELLED, new Timestamp(System.currentTimeMillis()));
        when(bookingService.cancelBooking(bookingId)).thenReturn(cancelledResponse);

        mockMvc.perform(patch("/booking/{id}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelBooking_AlreadyCancelled() throws Exception {
        when(bookingService.cancelBooking(bookingId))
                .thenThrow(new InvalidStatusTransitionException("Booking already cancelled"));

        mockMvc.perform(patch("/booking/{id}/cancel", bookingId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Booking already cancelled"));
    }

    @Test
    void cancelBooking_NotFound() throws Exception {
        when(bookingService.cancelBooking(bookingId))
                .thenThrow(new ResourceNotFoundException("Booking not found: " + bookingId));

        mockMvc.perform(patch("/booking/{id}/cancel", bookingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("Booking not found")));
    }

    @Test
    void getBookingsByLoadId_Success() throws Exception {
        when(bookingService.getBookingsByLoadId(loadId))
                .thenReturn(Collections.singletonList(testBookingResponse));

        mockMvc.perform(get("/booking/load/{loadId}", loadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bookingId").value(bookingId.toString()));
    }

    @Test
    void getBookingsByLoadId_Empty() throws Exception {
        when(bookingService.getBookingsByLoadId(loadId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/booking/load/{loadId}", loadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getBookingsByTransporterId_Success() throws Exception {
        when(bookingService.getBookingsByTransporterId(transporterId))
                .thenReturn(Collections.singletonList(testBookingResponse));

        mockMvc.perform(get("/booking/transporter/{transporterId}", transporterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bookingId").value(bookingId.toString()));
    }

    @Test
    void getBookingsByTransporterId_Empty() throws Exception {
        when(bookingService.getBookingsByTransporterId(transporterId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/booking/transporter/{transporterId}", transporterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
