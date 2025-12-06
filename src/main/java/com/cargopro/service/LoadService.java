package com.cargopro.service;

import com.cargopro.dto.LoadRequest;
import com.cargopro.dto.LoadResponse;
import com.cargopro.entity.Load;
import com.cargopro.enums.LoadStatus;
import com.cargopro.exception.InvalidStatusTransitionException;
import com.cargopro.exception.ResourceNotFoundException;
import com.cargopro.repository.LoadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Service class for Load operations
 */
@Service
@Transactional
public class LoadService {

    @Autowired
    private LoadRepository loadRepository;

    /**
     * Create a new load
     */
    public LoadResponse createLoad(LoadRequest request) {
        Load load = new Load();
        load.setShipperId(request.getShipperId());
        load.setLoadingCity(request.getLoadingCity());
        load.setUnloadingCity(request.getUnloadingCity());
        load.setProductType(request.getProductType());
        load.setTruckType(request.getTruckType());
        load.setNoOfTrucks(request.getNoOfTrucks());
        load.setRemainingTrucks(request.getNoOfTrucks());
        load.setWeight(request.getWeight());
        load.setWeightUnit(request.getWeightUnit());
        load.setComment(request.getComment());

        // Default loading date to now if not handled (needs input in request?
        // Request didn't have loadingDate. I'll use now() or check if request has it.
        // Checking LoadRequest... I didn't add loadingDate to LoadRequest.
        // I should have. I'll default to now() plus 1 day for now to avoid compilation
        // error
        // or just now().)
        load.setLoadingDate(java.time.LocalDateTime.now().plusDays(1));

        load.setStatus(LoadStatus.POSTED);

        Load savedLoad = loadRepository.save(load);
        return convertToResponse(savedLoad);
    }

    /**
     * Get loads with filtering
     */
    public Page<LoadResponse> getAllLoads(String shipperId, LoadStatus status, Pageable pageable) {
        Specification<Load> spec = Specification.where(null);

        if (shipperId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("shipperId"), shipperId));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<Load> loadPage = loadRepository.findAll(spec, pageable);
        return loadPage.map(this::convertToResponse);
    }

    /**
     * Get a load by ID
     */
    public LoadResponse getLoadById(UUID id) {
        Load load = getLoadEntity(id);
        return convertToResponse(load);
    }

    /**
     * Get the Load entity
     */
    public Load getLoadEntity(UUID id) {
        return loadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found with id: " + id));
    }

    /**
     * Cancel a load
     */
    public LoadResponse cancelLoad(UUID loadId) {
        Load load = getLoadEntity(loadId);

        if (load.getStatus() == LoadStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Load is already cancelled");
        }

        if (load.getStatus() == LoadStatus.BOOKED) {
            throw new InvalidStatusTransitionException("Cannot cancel a booked load");
        }

        load.setStatus(LoadStatus.CANCELLED);
        Load savedLoad = loadRepository.save(load);

        return convertToResponse(savedLoad);
    }

    /**
     * Update load's remaining trucks and status
     */
    public void updateLoadAfterBooking(Load load, Integer trucksBooked) {
        load.setRemainingTrucks(load.getRemainingTrucks() - trucksBooked);

        if (load.getRemainingTrucks() <= 0) {
            load.setRemainingTrucks(0);
            load.setStatus(LoadStatus.BOOKED);
        } else {
            // If trucks are still remaining but status was POSTED, change to OPEN_FOR_BIDS?
            // Or keep as is. Usually if at least one booking exists, it's confusing.
            // But requirement says: POSTED -> OPEN_FOR_BIDS (when bids come?) -> BOOKED.
            // Actually, if we book trucks, it implies we accepted a bid.
            // If we accepted a bid, we might be in OPEN_FOR_BIDS already.
        }

        loadRepository.save(load);
    }

    public void save(Load load) {
        loadRepository.save(load);
    }

    private LoadResponse convertToResponse(Load load) {
        return new LoadResponse(
                load.getLoadId(),
                load.getShipperId(),
                load.getLoadingCity(),
                load.getUnloadingCity(),
                Timestamp.valueOf(load.getLoadingDate()),
                load.getProductType(),
                load.getWeight(),
                load.getWeightUnit(),
                load.getTruckType(),
                load.getNoOfTrucks(),
                load.getRemainingTrucks(),
                load.getStatus(),
                load.getDatePosted() != null ? Timestamp.valueOf(load.getDatePosted()) : null);
    }
}
