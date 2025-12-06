package com.cargopro.controller;

import com.cargopro.dto.BidRequest;
import com.cargopro.dto.BidResponse;
import com.cargopro.service.BidService;
import com.cargopro.enums.BidStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bid")
@Tag(name = "Bid Management", description = "APIs for managing bids on loads")
public class BidController {

    @Autowired
    private BidService bidService;

    @PostMapping
    @Operation(summary = "Create a bid", description = "Transporter places a bid on a load")
    public ResponseEntity<BidResponse> createBid(@Valid @RequestBody BidRequest request) {
        // Transporter ID and Load ID are in the request body
        BidResponse response = bidService.createBid(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get bids", description = "Get bids filtered by loadId, transporterId, or status")
    public ResponseEntity<List<BidResponse>> getBids(
            @RequestParam(required = false) UUID loadId,
            @RequestParam(required = false) UUID transporterId,
            @RequestParam(required = false) BidStatus status) {
        List<BidResponse> bids = bidService.getBids(loadId, transporterId, status);
        return ResponseEntity.ok(bids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bid details", description = "Get full details of a bid")
    public ResponseEntity<BidResponse> getBidById(@PathVariable UUID id) {
        BidResponse bid = bidService.getBidById(id);
        return ResponseEntity.ok(bid);
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject bid", description = "Reject a pending bid")
    public ResponseEntity<BidResponse> rejectBid(@PathVariable UUID id) {
        BidResponse response = bidService.rejectBid(id);
        return ResponseEntity.ok(response);
    }
}
