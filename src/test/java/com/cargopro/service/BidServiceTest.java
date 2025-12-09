package com.cargopro.service;

import com.cargopro.dto.BestBidResponse;
import com.cargopro.dto.BidRequest;
import com.cargopro.dto.BidResponse;
import com.cargopro.entity.Bid;
import com.cargopro.entity.Load;
import com.cargopro.entity.Transporter;
import com.cargopro.entity.TruckAvailability;
import com.cargopro.enums.BidStatus;
import com.cargopro.enums.LoadStatus;
import com.cargopro.exception.InsufficientCapacityException;
import com.cargopro.exception.InvalidStatusTransitionException;
import com.cargopro.exception.ResourceNotFoundException;
import com.cargopro.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private LoadService loadService;

    @Mock
    private TransporterService transporterService;

    @InjectMocks
    private BidService bidService;

    private Load testLoad;
    private Transporter testTransporter;
    private Bid testBid;
    private UUID loadId;
    private UUID transporterId;
    private UUID bidId;

    @BeforeEach
    void setUp() {
        loadId = UUID.randomUUID();
        transporterId = UUID.randomUUID();
        bidId = UUID.randomUUID();

        testLoad = new Load();
        testLoad.setLoadId(loadId);
        testLoad.setStatus(LoadStatus.POSTED);
        testLoad.setTruckType("LARGE");
        testLoad.setRemainingTrucks(10);

        testTransporter = new Transporter();
        testTransporter.setTransporterId(transporterId);
        testTransporter.setCompanyName("Test Company");
        testTransporter.setRating(4.5);
        testTransporter.setAvailableTrucks(Arrays.asList(
                new TruckAvailability("LARGE", 20)));

        testBid = new Bid();
        testBid.setBidId(bidId);
        testBid.setLoad(testLoad);
        testBid.setTransporter(testTransporter);
        testBid.setProposedRate(500.0);
        testBid.setTrucksOffered(5);
        testBid.setStatus(BidStatus.PENDING);
        testBid.setSubmittedAt(LocalDateTime.now());
    }

    @Test
    void createBid_Success() {
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(500.0);
        request.setTrucksOffered(5);

        when(loadService.getLoadEntity(loadId)).thenReturn(testLoad);
        when(transporterService.getTransporterEntity(transporterId)).thenReturn(testTransporter);
        when(bidRepository.save(any(Bid.class))).thenReturn(testBid);

        BidResponse response = bidService.createBid(request);

        assertNotNull(response);
        assertEquals(500.0, response.getProposedRate());
        assertEquals(5, response.getTrucksOffered());
        verify(loadService).save(testLoad); // Status should be updated to OPEN_FOR_BIDS
        assertEquals(LoadStatus.OPEN_FOR_BIDS, testLoad.getStatus());
    }

    @Test
    void createBid_WhenLoadStatusOpenForBids_ShouldNotUpdateStatus() {
        testLoad.setStatus(LoadStatus.OPEN_FOR_BIDS);

        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(500.0);
        request.setTrucksOffered(5);

        when(loadService.getLoadEntity(loadId)).thenReturn(testLoad);
        when(transporterService.getTransporterEntity(transporterId)).thenReturn(testTransporter);
        when(bidRepository.save(any(Bid.class))).thenReturn(testBid);

        bidService.createBid(request);

        verify(loadService, never()).save(any()); // Status already OPEN_FOR_BIDS
    }

    @Test
    void createBid_WhenLoadIsCancelled_ShouldThrowException() {
        testLoad.setStatus(LoadStatus.CANCELLED);

        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(500.0);
        request.setTrucksOffered(5);

        when(loadService.getLoadEntity(loadId)).thenReturn(testLoad);
        when(transporterService.getTransporterEntity(transporterId)).thenReturn(testTransporter);

        assertThrows(InvalidStatusTransitionException.class, () -> bidService.createBid(request));
    }

    @Test
    void createBid_WhenLoadIsBooked_ShouldThrowException() {
        testLoad.setStatus(LoadStatus.BOOKED);

        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(500.0);
        request.setTrucksOffered(5);

        when(loadService.getLoadEntity(loadId)).thenReturn(testLoad);
        when(transporterService.getTransporterEntity(transporterId)).thenReturn(testTransporter);

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> bidService.createBid(request));
        assertTrue(exception.getMessage().contains("BOOKED"));
    }

    @Test
    void createBid_WhenInsufficientTransporterTrucks_ShouldThrowException() {
        testTransporter.setAvailableTrucks(Arrays.asList(
                new TruckAvailability("LARGE", 2) // Only 2 trucks available
        ));

        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(500.0);
        request.setTrucksOffered(5); // Requesting 5

        when(loadService.getLoadEntity(loadId)).thenReturn(testLoad);
        when(transporterService.getTransporterEntity(transporterId)).thenReturn(testTransporter);

        InsufficientCapacityException exception = assertThrows(
                InsufficientCapacityException.class,
                () -> bidService.createBid(request));
        assertTrue(exception.getMessage().contains("only has 2"));
    }

    @Test
    void createBid_WhenNoMatchingTruckType_ShouldThrowException() {
        testTransporter.setAvailableTrucks(Arrays.asList(
                new TruckAvailability("SMALL", 20) // Wrong type
        ));

        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(500.0);
        request.setTrucksOffered(5);

        when(loadService.getLoadEntity(loadId)).thenReturn(testLoad);
        when(transporterService.getTransporterEntity(transporterId)).thenReturn(testTransporter);

        InsufficientCapacityException exception = assertThrows(
                InsufficientCapacityException.class,
                () -> bidService.createBid(request));
        assertTrue(exception.getMessage().contains("only has 0"));
    }

    @Test
    void createBid_WhenExceedsRemainingTrucks_ShouldThrowException() {
        testLoad.setRemainingTrucks(3); // Only 3 remaining

        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(500.0);
        request.setTrucksOffered(5); // Offering 5

        when(loadService.getLoadEntity(loadId)).thenReturn(testLoad);
        when(transporterService.getTransporterEntity(transporterId)).thenReturn(testTransporter);

        InsufficientCapacityException exception = assertThrows(
                InsufficientCapacityException.class,
                () -> bidService.createBid(request));
        assertTrue(exception.getMessage().contains("remaining trucks"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBids_WithAllFilters_ShouldReturnFilteredBids() {
        when(bidRepository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(testBid));

        List<BidResponse> result = bidService.getBids(loadId, transporterId, BidStatus.PENDING);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(bidId, result.get(0).getBidId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBids_WithNoFilters_ShouldReturnAllBids() {
        when(bidRepository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(testBid));

        List<BidResponse> result = bidService.getBids(null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBids_WithLoadIdOnly_ShouldReturnFilteredBids() {
        when(bidRepository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(testBid));

        List<BidResponse> result = bidService.getBids(loadId, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBids_WithTransporterIdOnly_ShouldReturnFilteredBids() {
        when(bidRepository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(testBid));

        List<BidResponse> result = bidService.getBids(null, transporterId, null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBids_WithStatusOnly_ShouldReturnFilteredBids() {
        when(bidRepository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(testBid));

        List<BidResponse> result = bidService.getBids(null, null, BidStatus.PENDING);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getBidById_WhenBidExists_ShouldReturnBidResponse() {
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));

        BidResponse response = bidService.getBidById(bidId);

        assertNotNull(response);
        assertEquals(bidId, response.getBidId());
        assertEquals(500.0, response.getProposedRate());
    }

    @Test
    void getBidById_WhenBidNotFound_ShouldThrowException() {
        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bidService.getBidById(bidId));
    }

    @Test
    void getBidEntity_WhenBidExists_ShouldReturnBid() {
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));

        Bid result = bidService.getBidEntity(bidId);

        assertNotNull(result);
        assertEquals(bidId, result.getBidId());
    }

    @Test
    void getBidEntity_WhenBidNotFound_ShouldThrowException() {
        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bidService.getBidEntity(bidId));
        assertTrue(exception.getMessage().contains(bidId.toString()));
    }

    @Test
    void rejectBid_WhenBidIsPending_ShouldRejectSuccessfully() {
        testBid.setStatus(BidStatus.PENDING);
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));
        when(bidRepository.save(any(Bid.class))).thenReturn(testBid);

        BidResponse response = bidService.rejectBid(bidId);

        assertEquals(BidStatus.REJECTED, testBid.getStatus());
        verify(bidRepository).save(testBid);
    }

    @Test
    void rejectBid_WhenBidIsNotPending_ShouldThrowException() {
        testBid.setStatus(BidStatus.ACCEPTED);
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> bidService.rejectBid(bidId));
        assertTrue(exception.getMessage().contains("only reject pending"));
    }

    @Test
    void rejectBid_WhenBidIsAlreadyRejected_ShouldThrowException() {
        testBid.setStatus(BidStatus.REJECTED);
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));

        assertThrows(InvalidStatusTransitionException.class, () -> bidService.rejectBid(bidId));
    }

    @Test
    void getBestBidsForLoad_ShouldReturnSortedBids() {
        Bid bid1 = createBid(UUID.randomUUID(), 500.0, 5, 4.0); // Lower rate, good rating
        Bid bid2 = createBid(UUID.randomUUID(), 600.0, 5, 5.0); // Higher rate, best rating
        Bid bid3 = createBid(UUID.randomUUID(), 400.0, 5, 3.0); // Lowest rate, medium rating

        when(bidRepository.findByLoadLoadId(loadId)).thenReturn(Arrays.asList(bid1, bid2, bid3));

        List<BestBidResponse> result = bidService.getBestBidsForLoad(loadId);

        assertNotNull(result);
        assertEquals(3, result.size());
        // Should be sorted by score (descending) - lowest rate with decent rating
        // should score higher
        assertTrue(result.get(0).getScore() >= result.get(1).getScore());
        assertTrue(result.get(1).getScore() >= result.get(2).getScore());
    }

    @Test
    void getBestBidsForLoad_WhenNoBids_ShouldReturnEmptyList() {
        when(bidRepository.findByLoadLoadId(loadId)).thenReturn(Collections.emptyList());

        List<BestBidResponse> result = bidService.getBestBidsForLoad(loadId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void save_ShouldCallRepositorySave() {
        when(bidRepository.save(testBid)).thenReturn(testBid);

        bidService.save(testBid);

        verify(bidRepository, times(1)).save(testBid);
    }

    private Bid createBid(UUID id, double rate, int trucks, double rating) {
        Transporter t = new Transporter();
        t.setTransporterId(UUID.randomUUID());
        t.setCompanyName("Company");
        t.setRating(rating);

        Bid bid = new Bid();
        bid.setBidId(id);
        bid.setLoad(testLoad);
        bid.setTransporter(t);
        bid.setProposedRate(rate);
        bid.setTrucksOffered(trucks);
        bid.setStatus(BidStatus.PENDING);
        bid.setSubmittedAt(LocalDateTime.now());
        return bid;
    }
}
