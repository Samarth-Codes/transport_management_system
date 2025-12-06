package com.cargopro.service;

import com.cargopro.dto.BookingResponse;
import com.cargopro.entity.Booking;
import com.cargopro.enums.BookingStatus;
import com.cargopro.enums.BidStatus;
import com.cargopro.entity.Load;
import com.cargopro.entity.Load;
import com.cargopro.enums.LoadStatus;
import com.cargopro.exception.InvalidStatusTransitionException;
import com.cargopro.exception.ResourceNotFoundException;
import com.cargopro.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for Booking operations
 */
@Service
@Transactional
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BidService bidService;

    @Autowired
    private LoadService loadService;

    @Autowired
    private TransporterService transporterService;

    /**
     * Accept a bid and create a booking
     */
    public BookingResponse acceptBid(UUID bidId) {
        com.cargopro.entity.Bid bid = bidService.getBidEntity(bidId);
        Load load = bid.getLoad();
        com.cargopro.entity.Transporter transporter = bid.getTransporter();

        if (load.getStatus() != LoadStatus.POSTED && load.getStatus() != LoadStatus.OPEN_FOR_BIDS
                && load.getStatus() != LoadStatus.BOOKED) {
            throw new InvalidStatusTransitionException(
                    "Cannot accept bid for a load with status: " + load.getStatus());
        }

        if (load.getRemainingTrucks() <= 0) {
            throw new InvalidStatusTransitionException("Load is already fully booked");
        }

        // Reduce available trucks from transporter
        // Since we are accepting a bid, we assume the transporter *had* trucks when
        // bidding.
        // But we should check again or just try to reduce and catch exception.
        // TransporterService.reduceAvailableTrucks will throw if insufficient.
        transporterService.reduceAvailableTrucks(transporter.getTransporterId(), load.getTruckType(),
                bid.getTrucksOffered());

        if (bid.getTrucksOffered() > load.getRemainingTrucks()) {
            // Rollback transporter reduction? Or just fail?
            // Transactional will handle rollback if we throw exception.
            throw new InvalidStatusTransitionException(
                    "Cannot book more trucks than remaining trucks needed");
        }

        Booking booking = new Booking();
        booking.setLoad(load);
        booking.setBid(bid);
        booking.setTransporter(transporter);
        booking.setAllocatedTrucks(bid.getTrucksOffered());
        booking.setFinalRate(bid.getProposedRate() * bid.getTrucksOffered()); // Or just rate? usually rate is per
                                                                              // unit/truck? Schema said finalRate. I'll
                                                                              // assume total cost or per truck?
        // User schema: `proposedRate` (double). usually per truck or per load.
        // Given `trucksOffered` and `proposedRate`, usually rate is total for the
        // offered trucks?
        // Or rate per ton?
        // I will assume `proposedRate` is the rate for the *entire* offer.
        // So `finalRate` = `proposedRate`.
        // If proposedRate was per truck, then final = rate * trucks.
        // Let's assume proposedRate is the total price for the service.
        booking.setFinalRate(bid.getProposedRate());

        booking.setStatus(BookingStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);

        loadService.updateLoadAfterBooking(load, bid.getTrucksOffered());

        bid.setStatus(BidStatus.ACCEPTED);
        bidService.save(bid);

        return convertToResponse(savedBooking);
    }

    public List<BookingResponse> getBookingsByLoadId(UUID loadId) {
        List<Booking> bookings = bookingRepository.findByLoadLoadId(loadId);
        return bookings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByTransporterId(UUID transporterId) {
        List<Booking> bookings = bookingRepository.findByTransporterTransporterId(transporterId);
        return bookings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public BookingResponse getBookingById(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        return convertToResponse(booking);
    }

    public BookingResponse cancelBooking(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Booking already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        Load load = booking.getLoad();
        // Restore trucks to load
        load.setRemainingTrucks(load.getRemainingTrucks() + booking.getAllocatedTrucks());
        if (load.getStatus() == LoadStatus.BOOKED) {
            load.setStatus(LoadStatus.OPEN_FOR_BIDS);
        }
        loadService.save(load);

        // Restore trucks to transporter
        transporterService.restoreAvailableTrucks(
                booking.getTransporter().getTransporterId(),
                load.getTruckType(),
                booking.getAllocatedTrucks());

        return convertToResponse(booking);
    }

    private BookingResponse convertToResponse(Booking booking) {
        return new BookingResponse(
                booking.getBookingId(),
                booking.getLoad().getLoadId(),
                booking.getBid().getBidId(),
                booking.getTransporter().getTransporterId(),
                booking.getTransporter().getCompanyName(),
                booking.getAllocatedTrucks(),
                booking.getFinalRate(),
                booking.getStatus(),
                Timestamp.valueOf(booking.getBookedAt()));
    }
}
