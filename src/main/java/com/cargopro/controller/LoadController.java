package com.cargopro.controller;

import com.cargopro.dto.LoadRequest;
import com.cargopro.dto.LoadResponse;
import com.cargopro.service.LoadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cargopro.dto.BestBidResponse;
import com.cargopro.enums.LoadStatus;
import com.cargopro.service.BidService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/load")
@Tag(name = "Load Management", description = "APIs for managing shipping loads")
public class LoadController {

    @Autowired
    private LoadService loadService;

    @Autowired
    private BidService bidService;

    @PostMapping
    @Operation(summary = "Create a new load", description = "Creates a new shipping load that needs trucks")
    public ResponseEntity<LoadResponse> createLoad(@Valid @RequestBody LoadRequest request) {
        LoadResponse response = loadService.createLoad(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all loads", description = "Retrieves a list of all loads")
    public ResponseEntity<Page<LoadResponse>> getAllLoads(
            @RequestParam(required = false) String shipperId,
            @RequestParam(required = false) LoadStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LoadResponse> loads = loadService.getAllLoads(shipperId, status, pageable);
        return ResponseEntity.ok(loads);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get load by ID", description = "Retrieves a specific load by its ID")
    public ResponseEntity<LoadResponse> getLoadById(@PathVariable UUID id) {
        LoadResponse load = loadService.getLoadById(id);
        return ResponseEntity.ok(load);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a load", description = "Cancels an active load")
    public ResponseEntity<LoadResponse> cancelLoad(@PathVariable UUID id) {
        LoadResponse response = loadService.cancelLoad(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/best-bids")
    @Operation(summary = "Get best bids", description = "Get sorted bid suggestions for a load")
    public ResponseEntity<List<BestBidResponse>> getBestBids(@PathVariable UUID id) {
        List<BestBidResponse> bestBids = bidService.getBestBidsForLoad(id);
        return ResponseEntity.ok(bestBids);
    }
}
