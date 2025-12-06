package com.cargopro.service;

import com.cargopro.dto.BestBidResponse;
import com.cargopro.dto.BidRequest;
import com.cargopro.dto.BidResponse;
import com.cargopro.entity.Bid;
import com.cargopro.enums.BidStatus;
import com.cargopro.entity.Load;
import com.cargopro.entity.Transporter;
import com.cargopro.entity.TruckAvailability;
import com.cargopro.enums.LoadStatus;
import com.cargopro.exception.InsufficientCapacityException;
import com.cargopro.exception.InvalidStatusTransitionException;
import com.cargopro.exception.ResourceNotFoundException;
import com.cargopro.repository.BidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for Bid operations
 */
@Service
@Transactional
public class BidService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private LoadService loadService;

    @Autowired
    private TransporterService transporterService;

    /**
     * Create a new bid
     */
    public BidResponse createBid(BidRequest request) {
        // Get the load and transporter entities
        Load load = loadService.getLoadEntity(request.getLoadId());
        Transporter transporter = transporterService.getTransporterEntity(request.getTransporterId());

        // Validate that load is active
        if (load.getStatus() != LoadStatus.POSTED && load.getStatus() != LoadStatus.OPEN_FOR_BIDS) {
            throw new InvalidStatusTransitionException(
                    "Cannot bid on a load with status: " + load.getStatus());
        }

        // Validate that transporter has enough trucks
        // Need to check for the specific truck type required by the load
        String requiredTruckType = load.getTruckType();

        // Find availability
        int availableCount = transporter.getAvailableTrucks().stream()
                .filter(ta -> ta.getTruckType().equalsIgnoreCase(requiredTruckType))
                .mapToInt(TruckAvailability::getCount)
                .findFirst()
                .orElse(0);

        if (availableCount < request.getTrucksOffered()) {
            throw new InsufficientCapacityException(
                    "Transporter only has " + availableCount +
                            " available trucks of type " + requiredTruckType + ", but trying to offer "
                            + request.getTrucksOffered());
        }

        // Validate that trucks offered doesn't exceed remaining trucks needed
        if (request.getTrucksOffered() > load.getRemainingTrucks()) {
            throw new InsufficientCapacityException(
                    "Cannot offer more trucks (" + request.getTrucksOffered() +
                            ") than remaining trucks needed (" + load.getRemainingTrucks() + ")");
        }

        // Create new Bid entity
        Bid bid = new Bid();
        bid.setLoad(load);
        bid.setTransporter(transporter);
        bid.setProposedRate(request.getProposedRate());
        bid.setTrucksOffered(request.getTrucksOffered());
        bid.setStatus(BidStatus.PENDING);

        // Rule: POSTED -> OPEN_FOR_BIDS
        if (load.getStatus() == LoadStatus.POSTED) {
            load.setStatus(LoadStatus.OPEN_FOR_BIDS);
            loadService.save(load);
        }

        Bid savedBid = bidRepository.save(bid);
        return convertToResponse(savedBid);
    }

    public List<BidResponse> getBids(UUID loadId, UUID transporterId, BidStatus status) {
        Specification<Bid> spec = Specification.where(null);

        if (loadId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("load").get("loadId"), loadId));
        }

        if (transporterId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("transporter").get("transporterId"), transporterId));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return bidRepository.findAll(spec).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a bid by ID
     */
    public BidResponse getBidById(UUID id) {
        Bid bid = getBidEntity(id);
        return convertToResponse(bid);
    }

    /**
     * Reject a bid
     */
    public BidResponse rejectBid(UUID id) {
        Bid bid = getBidEntity(id);

        if (bid.getStatus() != BidStatus.PENDING) {
            throw new InvalidStatusTransitionException("Can only reject pending bids");
        }

        bid.setStatus(BidStatus.REJECTED);
        Bid savedBid = bidRepository.save(bid);
        return convertToResponse(savedBid);
    }

    /**
     * Get best bids for a load sorted by score
     */
    public List<BestBidResponse> getBestBidsForLoad(UUID loadId) {
        List<Bid> bids = bidRepository.findByLoadLoadId(loadId);

        return bids.stream()
                .map(bid -> {
                    // Higher score = better bid (custom formula needed)
                    // Let's assume lower rate is better, higher rating is better.
                    // Simple score: (Rating / 5) * 1000 - Rate
                    // Or user previous formula: (1.0 / rate) * 0.7 + (rating / 5.0) * 0.3

                    double rateScore = (1.0 / bid.getProposedRate()) * 0.7;
                    double ratingScore = (bid.getTransporter().getRating() / 5.0) * 0.3;
                    double score = rateScore + ratingScore;

                    return new BestBidResponse(
                            bid.getBidId(),
                            bid.getTransporter().getTransporterId(),
                            bid.getTransporter().getCompanyName(),
                            bid.getTransporter().getRating(),
                            bid.getProposedRate(),
                            bid.getTrucksOffered(),
                            score,
                            Timestamp.valueOf(bid.getSubmittedAt()));
                })
                .sorted(Comparator.comparing(BestBidResponse::getScore).reversed()) // Descending score
                .collect(Collectors.toList());
    }

    public void save(Bid bid) {
        bidRepository.save(bid);
    }

    public Bid getBidEntity(UUID bidId) {
        return bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found with id: " + bidId));
    }

    private BidResponse convertToResponse(Bid bid) {
        return new BidResponse(
                bid.getBidId(),
                bid.getProposedRate(),
                bid.getTrucksOffered(),
                bid.getLoad().getLoadId(),
                bid.getTransporter().getTransporterId(),
                bid.getTransporter().getCompanyName(),
                bid.getTransporter().getRating(),
                Timestamp.valueOf(bid.getSubmittedAt()),
                bid.getStatus());
    }
}
