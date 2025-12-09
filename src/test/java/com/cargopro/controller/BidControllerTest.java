package com.cargopro.controller;

import com.cargopro.dto.BidRequest;
import com.cargopro.dto.BidResponse;
import com.cargopro.enums.BidStatus;
import com.cargopro.exception.InsufficientCapacityException;
import com.cargopro.exception.InvalidStatusTransitionException;
import com.cargopro.exception.ResourceNotFoundException;
import com.cargopro.service.BidService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BidController.class)
class BidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BidService bidService;

    private BidResponse testBidResponse;
    private UUID bidId;
    private UUID loadId;
    private UUID transporterId;

    @BeforeEach
    void setUp() {
        bidId = UUID.randomUUID();
        loadId = UUID.randomUUID();
        transporterId = UUID.randomUUID();
        testBidResponse = new BidResponse(
                bidId,
                500.0,
                5,
                loadId,
                transporterId,
                "Test Company",
                4.5,
                new Timestamp(System.currentTimeMillis()),
                BidStatus.PENDING);
    }

    @Test
    void createBid_Success() throws Exception {
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(500.0);
        request.setTrucksOffered(5);

        when(bidService.createBid(any(BidRequest.class))).thenReturn(testBidResponse);

        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bidId").value(bidId.toString()))
                .andExpect(jsonPath("$.proposedRate").value(500.0))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createBid_ValidationError_MissingLoadId() throws Exception {
        BidRequest request = new BidRequest();
        // Missing loadId
        request.setTransporterId(transporterId);
        request.setProposedRate(500.0);
        request.setTrucksOffered(5);

        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.loadId").value("Load ID is required"));
    }

    @Test
    void createBid_ValidationError_MissingTransporterId() throws Exception {
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        // Missing transporterId
        request.setProposedRate(500.0);
        request.setTrucksOffered(5);

        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.transporterId").value("Transporter ID is required"));
    }

    @Test
    void createBid_ValidationError_NegativeRate() throws Exception {
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(-100.0); // Invalid
        request.setTrucksOffered(5);

        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.proposedRate").value("Proposed rate must be positive"));
    }

    @Test
    void createBid_ValidationError_NegativeTrucks() throws Exception {
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(500.0);
        request.setTrucksOffered(-1); // Invalid

        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.trucksOffered").value("Trucks offered must be positive"));
    }

    @Test
    void createBid_InsufficientCapacity() throws Exception {
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(500.0);
        request.setTrucksOffered(100);

        when(bidService.createBid(any(BidRequest.class)))
                .thenThrow(new InsufficientCapacityException("Not enough trucks available"));

        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Not enough trucks available"));
    }

    @Test
    void createBid_InvalidLoadStatus() throws Exception {
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(500.0);
        request.setTrucksOffered(5);

        when(bidService.createBid(any(BidRequest.class)))
                .thenThrow(new InvalidStatusTransitionException("Cannot bid on a cancelled load"));

        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot bid on a cancelled load"));
    }

    @Test
    void getBids_Success() throws Exception {
        when(bidService.getBids(any(), any(), any())).thenReturn(Collections.singletonList(testBidResponse));

        mockMvc.perform(get("/bid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bidId").value(bidId.toString()));
    }

    @Test
    void getBids_WithFilters() throws Exception {
        when(bidService.getBids(eq(loadId), eq(transporterId), eq(BidStatus.PENDING)))
                .thenReturn(Collections.singletonList(testBidResponse));

        mockMvc.perform(get("/bid")
                .param("loadId", loadId.toString())
                .param("transporterId", transporterId.toString())
                .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getBids_Empty() throws Exception {
        when(bidService.getBids(any(), any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/bid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getBidById_Success() throws Exception {
        when(bidService.getBidById(bidId)).thenReturn(testBidResponse);

        mockMvc.perform(get("/bid/{id}", bidId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidId").value(bidId.toString()))
                .andExpect(jsonPath("$.proposedRate").value(500.0));
    }

    @Test
    void getBidById_NotFound() throws Exception {
        when(bidService.getBidById(bidId))
                .thenThrow(new ResourceNotFoundException("Bid not found with id: " + bidId));

        mockMvc.perform(get("/bid/{id}", bidId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("Bid not found")));
    }

    @Test
    void rejectBid_Success() throws Exception {
        BidResponse rejectedResponse = new BidResponse(
                bidId, 500.0, 5, loadId, transporterId, "Test Company", 4.5,
                new Timestamp(System.currentTimeMillis()), BidStatus.REJECTED);
        when(bidService.rejectBid(bidId)).thenReturn(rejectedResponse);

        mockMvc.perform(patch("/bid/{id}/reject", bidId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void rejectBid_NotPending() throws Exception {
        when(bidService.rejectBid(bidId))
                .thenThrow(new InvalidStatusTransitionException("Can only reject pending bids"));

        mockMvc.perform(patch("/bid/{id}/reject", bidId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Can only reject pending bids"));
    }

    @Test
    void rejectBid_NotFound() throws Exception {
        when(bidService.rejectBid(bidId))
                .thenThrow(new ResourceNotFoundException("Bid not found with id: " + bidId));

        mockMvc.perform(patch("/bid/{id}/reject", bidId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("Bid not found")));
    }
}
