package com.cargopro.service;

import com.cargopro.dto.TransporterRequest;
import com.cargopro.dto.TransporterResponse;
import com.cargopro.entity.Transporter;
import com.cargopro.entity.TruckAvailability;
import com.cargopro.exception.InsufficientCapacityException;
import com.cargopro.exception.ResourceNotFoundException;
import com.cargopro.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransporterServiceTest {

    @Mock
    private TransporterRepository transporterRepository;

    @InjectMocks
    private TransporterService transporterService;

    private Transporter testTransporter;
    private UUID transporterId;

    @BeforeEach
    void setUp() {
        transporterId = UUID.randomUUID();

        testTransporter = new Transporter();
        testTransporter.setTransporterId(transporterId);
        testTransporter.setCompanyName("Test Logistics");
        testTransporter.setRating(4.5);
        testTransporter.setAvailableTrucks(new ArrayList<>(Arrays.asList(
                new TruckAvailability("LARGE", 10),
                new TruckAvailability("MEDIUM", 5))));
    }

    @Test
    void createTransporter_ShouldReturnTransporterResponse() {
        TransporterRequest request = new TransporterRequest();
        request.setCompanyName("Test Logistics");
        request.setRating(4.5);
        request.setAvailableTrucks(Arrays.asList(
                new TruckAvailability("LARGE", 10)));

        when(transporterRepository.save(any(Transporter.class))).thenReturn(testTransporter);

        TransporterResponse response = transporterService.createTransporter(request);

        assertNotNull(response);
        assertEquals("Test Logistics", response.getCompanyName());
        assertEquals(4.5, response.getRating());
        verify(transporterRepository, times(1)).save(any(Transporter.class));
    }

    @Test
    void getAllTransporters_ShouldReturnAllTransporters() {
        Transporter transporter2 = new Transporter();
        transporter2.setTransporterId(UUID.randomUUID());
        transporter2.setCompanyName("Another Logistics");
        transporter2.setRating(3.5);
        transporter2.setAvailableTrucks(new ArrayList<>());

        when(transporterRepository.findAll()).thenReturn(Arrays.asList(testTransporter, transporter2));

        List<TransporterResponse> result = transporterService.getAllTransporters();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Logistics", result.get(0).getCompanyName());
        assertEquals("Another Logistics", result.get(1).getCompanyName());
    }

    @Test
    void getAllTransporters_WhenNoTransporters_ShouldReturnEmptyList() {
        when(transporterRepository.findAll()).thenReturn(Collections.emptyList());

        List<TransporterResponse> result = transporterService.getAllTransporters();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getTransporterById_WhenTransporterExists_ShouldReturnTransporterResponse() {
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));

        TransporterResponse response = transporterService.getTransporterById(transporterId);

        assertNotNull(response);
        assertEquals(transporterId, response.getTransporterId());
        assertEquals("Test Logistics", response.getCompanyName());
    }

    @Test
    void getTransporterById_WhenTransporterNotFound_ShouldThrowException() {
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transporterService.getTransporterById(transporterId));
    }

    @Test
    void getTransporterEntity_WhenTransporterExists_ShouldReturnTransporter() {
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));

        Transporter result = transporterService.getTransporterEntity(transporterId);

        assertNotNull(result);
        assertEquals(transporterId, result.getTransporterId());
    }

    @Test
    void getTransporterEntity_WhenTransporterNotFound_ShouldThrowException() {
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> transporterService.getTransporterEntity(transporterId));
        assertTrue(exception.getMessage().contains(transporterId.toString()));
    }

    @Test
    void updateAvailableTrucks_ShouldUpdateTrucks() {
        List<TruckAvailability> newTrucks = Arrays.asList(
                new TruckAvailability("SMALL", 15),
                new TruckAvailability("LARGE", 25));

        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));
        when(transporterRepository.save(any(Transporter.class))).thenReturn(testTransporter);

        TransporterResponse response = transporterService.updateAvailableTrucks(transporterId, newTrucks);

        assertNotNull(response);
        assertEquals(newTrucks, testTransporter.getAvailableTrucks());
        verify(transporterRepository).save(testTransporter);
    }

    @Test
    void reduceAvailableTrucks_Success() {
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));
        when(transporterRepository.save(any(Transporter.class))).thenReturn(testTransporter);

        transporterService.reduceAvailableTrucks(transporterId, "LARGE", 3);

        // Find LARGE truck availability and verify count reduced
        TruckAvailability largeTrucks = testTransporter.getAvailableTrucks().stream()
                .filter(t -> t.getTruckType().equalsIgnoreCase("LARGE"))
                .findFirst().orElseThrow();
        assertEquals(7, largeTrucks.getCount()); // 10 - 3 = 7
        verify(transporterRepository).save(testTransporter);
    }

    @Test
    void reduceAvailableTrucks_CaseInsensitive_ShouldWork() {
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));
        when(transporterRepository.save(any(Transporter.class))).thenReturn(testTransporter);

        transporterService.reduceAvailableTrucks(transporterId, "large", 3);

        TruckAvailability largeTrucks = testTransporter.getAvailableTrucks().stream()
                .filter(t -> t.getTruckType().equalsIgnoreCase("LARGE"))
                .findFirst().orElseThrow();
        assertEquals(7, largeTrucks.getCount());
    }

    @Test
    void reduceAvailableTrucks_WhenInsufficientTrucks_ShouldThrowException() {
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));

        InsufficientCapacityException exception = assertThrows(
                InsufficientCapacityException.class,
                () -> transporterService.reduceAvailableTrucks(transporterId, "LARGE", 15));
        assertTrue(exception.getMessage().contains("Not enough trucks"));
    }

    @Test
    void reduceAvailableTrucks_WhenTruckTypeNotFound_ShouldThrowException() {
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));

        InsufficientCapacityException exception = assertThrows(
                InsufficientCapacityException.class,
                () -> transporterService.reduceAvailableTrucks(transporterId, "EXTRA_LARGE", 5));
        assertTrue(exception.getMessage().contains("does not have trucks of type"));
    }

    @Test
    void restoreAvailableTrucks_WhenTypeExists_ShouldIncreaseCount() {
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));
        when(transporterRepository.save(any(Transporter.class))).thenReturn(testTransporter);

        transporterService.restoreAvailableTrucks(transporterId, "LARGE", 5);

        TruckAvailability largeTrucks = testTransporter.getAvailableTrucks().stream()
                .filter(t -> t.getTruckType().equalsIgnoreCase("LARGE"))
                .findFirst().orElseThrow();
        assertEquals(15, largeTrucks.getCount()); // 10 + 5 = 15
        verify(transporterRepository).save(testTransporter);
    }

    @Test
    void restoreAvailableTrucks_CaseInsensitive_ShouldWork() {
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));
        when(transporterRepository.save(any(Transporter.class))).thenReturn(testTransporter);

        transporterService.restoreAvailableTrucks(transporterId, "large", 5);

        TruckAvailability largeTrucks = testTransporter.getAvailableTrucks().stream()
                .filter(t -> t.getTruckType().equalsIgnoreCase("LARGE"))
                .findFirst().orElseThrow();
        assertEquals(15, largeTrucks.getCount());
    }

    @Test
    void restoreAvailableTrucks_WhenTypeNotExists_ShouldAddNewEntry() {
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));
        when(transporterRepository.save(any(Transporter.class))).thenReturn(testTransporter);

        int initialSize = testTransporter.getAvailableTrucks().size();
        transporterService.restoreAvailableTrucks(transporterId, "EXTRA_LARGE", 5);

        assertEquals(initialSize + 1, testTransporter.getAvailableTrucks().size());
        TruckAvailability extraLarge = testTransporter.getAvailableTrucks().stream()
                .filter(t -> t.getTruckType().equalsIgnoreCase("EXTRA_LARGE"))
                .findFirst().orElseThrow();
        assertEquals(5, extraLarge.getCount());
        verify(transporterRepository).save(testTransporter);
    }
}
