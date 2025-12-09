package com.cargopro.service;

import com.cargopro.dto.BookingResponse;
import com.cargopro.entity.Bid;
import com.cargopro.entity.Booking;
import com.cargopro.entity.Load;
import com.cargopro.entity.Transporter;
import com.cargopro.entity.TruckAvailability;
import com.cargopro.enums.BidStatus;
import com.cargopro.enums.BookingStatus;
import com.cargopro.enums.LoadStatus;
import com.cargopro.exception.InvalidStatusTransitionException;
import com.cargopro.exception.ResourceNotFoundException;
import com.cargopro.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BidService bidService;

    @Mock
    private LoadService loadService;

    @Mock
    private TransporterService transporterService;

    @InjectMocks
    private BookingService bookingService;

    private Load testLoad;
    private Transporter testTransporter;
    private Bid testBid;
    private Booking testBooking;
    private UUID loadId;
    private UUID transporterId;
    private UUID bidId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        loadId = UUID.randomUUID();
        transporterId = UUID.randomUUID();
        bidId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        testLoad = new Load();
        testLoad.setLoadId(loadId);
        testLoad.setStatus(LoadStatus.OPEN_FOR_BIDS);
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

        testBooking = new Booking();
        testBooking.setBookingId(bookingId);
        testBooking.setLoad(testLoad);
        testBooking.setBid(testBid);
        testBooking.setTransporter(testTransporter);
        testBooking.setAllocatedTrucks(5);
        testBooking.setFinalRate(500.0);
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setBookedAt(LocalDateTime.now());
    }

    @Test
    void acceptBid_Success() {
        when(bidService.getBidEntity(bidId)).thenReturn(testBid);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        doNothing().when(transporterService).reduceAvailableTrucks(transporterId, "LARGE", 5);
        doNothing().when(loadService).updateLoadAfterBooking(testLoad, 5);
        doNothing().when(bidService).save(any(Bid.class));

        BookingResponse response = bookingService.acceptBid(bidId);

        assertNotNull(response);
        assertEquals(bookingId, response.getBookingId());
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        verify(transporterService).reduceAvailableTrucks(transporterId, "LARGE", 5);
        verify(loadService).updateLoadAfterBooking(testLoad, 5);
        assertEquals(BidStatus.ACCEPTED, testBid.getStatus());
    }

    @Test
    void acceptBid_WhenLoadStatusPosted_ShouldSucceed() {
        testLoad.setStatus(LoadStatus.POSTED);
        when(bidService.getBidEntity(bidId)).thenReturn(testBid);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        doNothing().when(transporterService).reduceAvailableTrucks(any(), any(), anyInt());
        doNothing().when(loadService).updateLoadAfterBooking(any(), anyInt());

        BookingResponse response = bookingService.acceptBid(bidId);

        assertNotNull(response);
    }

    @Test
    void acceptBid_WhenLoadStatusBooked_ShouldSucceed() {
        // BOOKED status is allowed (for partial bookings)
        testLoad.setStatus(LoadStatus.BOOKED);
        testLoad.setRemainingTrucks(5); // Still has trucks
        when(bidService.getBidEntity(bidId)).thenReturn(testBid);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        doNothing().when(transporterService).reduceAvailableTrucks(any(), any(), anyInt());
        doNothing().when(loadService).updateLoadAfterBooking(any(), anyInt());

        BookingResponse response = bookingService.acceptBid(bidId);

        assertNotNull(response);
    }

    @Test
    void acceptBid_WhenLoadIsCancelled_ShouldThrowException() {
        testLoad.setStatus(LoadStatus.CANCELLED);
        when(bidService.getBidEntity(bidId)).thenReturn(testBid);

        assertThrows(InvalidStatusTransitionException.class, () -> bookingService.acceptBid(bidId));
    }

    @Test
    void acceptBid_WhenLoadFullyBooked_ShouldThrowException() {
        testLoad.setRemainingTrucks(0);
        when(bidService.getBidEntity(bidId)).thenReturn(testBid);

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> bookingService.acceptBid(bidId));
        assertTrue(exception.getMessage().contains("fully booked"));
    }

    @Test
    void acceptBid_WhenBidExceedsRemainingTrucks_ShouldThrowException() {
        testLoad.setRemainingTrucks(3);
        testBid.setTrucksOffered(5);
        when(bidService.getBidEntity(bidId)).thenReturn(testBid);
        doNothing().when(transporterService).reduceAvailableTrucks(any(), any(), anyInt());

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> bookingService.acceptBid(bidId));
        assertTrue(exception.getMessage().contains("more trucks than remaining"));
    }

    @Test
    void getBookingById_WhenBookingExists_ShouldReturnBookingResponse() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        BookingResponse response = bookingService.getBookingById(bookingId);

        assertNotNull(response);
        assertEquals(bookingId, response.getBookingId());
        assertEquals(5, response.getAllocatedTrucks());
    }

    @Test
    void getBookingById_WhenBookingNotFound_ShouldThrowException() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.getBookingById(bookingId));
    }

    @Test
    void getBookingsByLoadId_ShouldReturnBookings() {
        when(bookingRepository.findByLoadLoadId(loadId)).thenReturn(Collections.singletonList(testBooking));

        List<BookingResponse> result = bookingService.getBookingsByLoadId(loadId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(bookingId, result.get(0).getBookingId());
    }

    @Test
    void getBookingsByLoadId_WhenNoBookings_ShouldReturnEmptyList() {
        when(bookingRepository.findByLoadLoadId(loadId)).thenReturn(Collections.emptyList());

        List<BookingResponse> result = bookingService.getBookingsByLoadId(loadId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getBookingsByTransporterId_ShouldReturnBookings() {
        when(bookingRepository.findByTransporterTransporterId(transporterId))
                .thenReturn(Collections.singletonList(testBooking));

        List<BookingResponse> result = bookingService.getBookingsByTransporterId(transporterId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(bookingId, result.get(0).getBookingId());
    }

    @Test
    void getBookingsByTransporterId_WhenNoBookings_ShouldReturnEmptyList() {
        when(bookingRepository.findByTransporterTransporterId(transporterId))
                .thenReturn(Collections.emptyList());

        List<BookingResponse> result = bookingService.getBookingsByTransporterId(transporterId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void cancelBooking_Success() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testLoad.setStatus(LoadStatus.BOOKED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        doNothing().when(loadService).save(any(Load.class));
        doNothing().when(transporterService).restoreAvailableTrucks(any(), any(), anyInt());

        BookingResponse response = bookingService.cancelBooking(bookingId);

        assertEquals(BookingStatus.CANCELLED, testBooking.getStatus());
        assertEquals(LoadStatus.OPEN_FOR_BIDS, testLoad.getStatus());
        assertEquals(15, testLoad.getRemainingTrucks()); // 10 + 5 restored
        verify(transporterService).restoreAvailableTrucks(transporterId, "LARGE", 5);
    }

    @Test
    void cancelBooking_WhenLoadNotBooked_ShouldNotChangeLoadStatus() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testLoad.setStatus(LoadStatus.OPEN_FOR_BIDS); // Not fully booked
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        doNothing().when(loadService).save(any(Load.class));
        doNothing().when(transporterService).restoreAvailableTrucks(any(), any(), anyInt());

        bookingService.cancelBooking(bookingId);

        assertEquals(LoadStatus.OPEN_FOR_BIDS, testLoad.getStatus()); // Unchanged
    }

    @Test
    void cancelBooking_WhenAlreadyCancelled_ShouldThrowException() {
        testBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> bookingService.cancelBooking(bookingId));
        assertTrue(exception.getMessage().contains("already cancelled"));
    }

    @Test
    void cancelBooking_WhenBookingNotFound_ShouldThrowException() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.cancelBooking(bookingId));
    }
}
