package com.cargopro.service;

import com.cargopro.dto.TransporterRequest;
import com.cargopro.dto.TransporterResponse;
import com.cargopro.entity.Transporter;
import com.cargopro.entity.TruckAvailability;
import com.cargopro.exception.InsufficientCapacityException;
import com.cargopro.exception.ResourceNotFoundException;
import com.cargopro.repository.TransporterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for Transporter operations
 */
@Service
@Transactional
public class TransporterService {

    @Autowired
    private TransporterRepository transporterRepository;

    /**
     * Create a new transporter
     */
    public TransporterResponse createTransporter(TransporterRequest request) {
        Transporter transporter = new Transporter();
        transporter.setCompanyName(request.getCompanyName());
        transporter.setRating(request.getRating());
        transporter.setAvailableTrucks(request.getAvailableTrucks());

        Transporter savedTransporter = transporterRepository.save(transporter);
        return convertToResponse(savedTransporter);
    }

    /**
     * Get all transporters
     */
    public List<TransporterResponse> getAllTransporters() {
        List<Transporter> transporters = transporterRepository.findAll();
        return transporters.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a transporter by ID
     */
    public TransporterResponse getTransporterById(UUID id) {
        Transporter transporter = getTransporterEntity(id);
        return convertToResponse(transporter);
    }

    /**
     * Get the Transporter entity
     */
    public Transporter getTransporterEntity(UUID id) {
        return transporterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transporter not found with id: " + id));
    }

    /**
     * Update available trucks for a transporter
     */
    public TransporterResponse updateAvailableTrucks(UUID transporterId, List<TruckAvailability> trucks) {
        Transporter transporter = getTransporterEntity(transporterId);
        transporter.setAvailableTrucks(trucks);
        Transporter savedTransporter = transporterRepository.save(transporter);
        return convertToResponse(savedTransporter);
    }

    /**
     * Reduce available trucks when a booking is made
     */
    public void reduceAvailableTrucks(UUID transporterId, String truckType, int count) {
        Transporter transporter = getTransporterEntity(transporterId);

        // Find availability for the specific truck type
        Optional<TruckAvailability> availabilityOpt = transporter.getAvailableTrucks().stream()
                .filter(ta -> ta.getTruckType().equalsIgnoreCase(truckType))
                .findFirst();

        if (availabilityOpt.isPresent()) {
            TruckAvailability availability = availabilityOpt.get();
            if (availability.getCount() < count) {
                throw new InsufficientCapacityException("Not enough trucks of type " + truckType + " available");
            }
            availability.setCount(availability.getCount() - count);
        } else {
            throw new InsufficientCapacityException("Transporter does not have trucks of type " + truckType);
        }

        transporterRepository.save(transporter);
    }

    /**
     * Restore trucks when a load is cancelled
     */
    public void restoreAvailableTrucks(UUID transporterId, String truckType, int count) {
        Transporter transporter = getTransporterEntity(transporterId);

        Optional<TruckAvailability> availabilityOpt = transporter.getAvailableTrucks().stream()
                .filter(ta -> ta.getTruckType().equalsIgnoreCase(truckType))
                .findFirst();

        if (availabilityOpt.isPresent()) {
            TruckAvailability availability = availabilityOpt.get();
            availability.setCount(availability.getCount() + count);
        } else {
            // Restore by adding new availability entry if it was somehow removed?
            // Or just add it.
            transporter.getAvailableTrucks().add(new TruckAvailability(truckType, count));
        }

        transporterRepository.save(transporter);
    }

    private TransporterResponse convertToResponse(Transporter transporter) {
        return new TransporterResponse(
                transporter.getTransporterId(),
                transporter.getCompanyName(),
                transporter.getRating(),
                transporter.getAvailableTrucks());
    }
}
