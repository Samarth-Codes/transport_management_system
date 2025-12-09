package com.cargopro.service;

import com.cargopro.dto.LoadRequest;
import com.cargopro.dto.LoadResponse;
import com.cargopro.entity.Load;
import com.cargopro.enums.LoadStatus;
import com.cargopro.enums.WeightUnit;
import com.cargopro.exception.InvalidStatusTransitionException;
import com.cargopro.exception.ResourceNotFoundException;
import com.cargopro.repository.LoadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadServiceTest {

    @Mock
    private LoadRepository loadRepository;

    @InjectMocks
    private LoadService loadService;

    private Load testLoad;
    private UUID loadId;

    @BeforeEach
    void setUp() {
        loadId = UUID.randomUUID();
        testLoad = new Load();
        testLoad.setLoadId(loadId);
        testLoad.setShipperId("shipper-123");
        testLoad.setLoadingCity("New York");
        testLoad.setUnloadingCity("Los Angeles");
        testLoad.setProductType("Electronics");
        testLoad.setTruckType("LARGE");
        testLoad.setNoOfTrucks(10);
        testLoad.setRemainingTrucks(10);
        testLoad.setWeight(1000.0);
        testLoad.setWeightUnit(WeightUnit.KG);
        testLoad.setStatus(LoadStatus.POSTED);
        testLoad.setLoadingDate(LocalDateTime.now().plusDays(1));
        testLoad.setDatePosted(LocalDateTime.now());
    }

    @Test
    void createLoad_ShouldReturnLoadResponse() {
        LoadRequest request = new LoadRequest();
        request.setShipperId("shipper-123");
        request.setLoadingCity("New York");
        request.setUnloadingCity("Los Angeles");
        request.setProductType("Electronics");
        request.setTruckType("LARGE");
        request.setNoOfTrucks(10);
        request.setWeight(1000.0);
        request.setWeightUnit(WeightUnit.KG);

        when(loadRepository.save(any(Load.class))).thenReturn(testLoad);

        LoadResponse response = loadService.createLoad(request);

        assertNotNull(response);
        assertEquals("New York", response.getLoadingCity());
        assertEquals(LoadStatus.POSTED, response.getStatus());
        assertEquals(10, response.getRemainingTrucks());
        verify(loadRepository, times(1)).save(any(Load.class));
    }

    @Test
    void getLoadById_WhenLoadExists_ShouldReturnLoadResponse() {
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));

        LoadResponse response = loadService.getLoadById(loadId);

        assertNotNull(response);
        assertEquals(loadId, response.getLoadId());
        assertEquals("New York", response.getLoadingCity());
    }

    @Test
    void getLoadById_WhenLoadNotFound_ShouldThrowException() {
        when(loadRepository.findById(loadId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> loadService.getLoadById(loadId));
    }

    @Test
    void getLoadEntity_WhenLoadExists_ShouldReturnLoad() {
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));

        Load result = loadService.getLoadEntity(loadId);

        assertNotNull(result);
        assertEquals(loadId, result.getLoadId());
    }

    @Test
    void getLoadEntity_WhenLoadNotFound_ShouldThrowException() {
        when(loadRepository.findById(loadId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> loadService.getLoadEntity(loadId));
        assertTrue(exception.getMessage().contains(loadId.toString()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllLoads_WithNoFilters_ShouldReturnAllLoads() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Load> loadPage = new PageImpl<>(Collections.singletonList(testLoad));

        when(loadRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(loadPage);

        Page<LoadResponse> result = loadService.getAllLoads(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(loadRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllLoads_WithShipperIdFilter_ShouldFilterResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Load> loadPage = new PageImpl<>(Collections.singletonList(testLoad));

        when(loadRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(loadPage);

        Page<LoadResponse> result = loadService.getAllLoads("shipper-123", null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllLoads_WithStatusFilter_ShouldFilterResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Load> loadPage = new PageImpl<>(Collections.singletonList(testLoad));

        when(loadRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(loadPage);

        Page<LoadResponse> result = loadService.getAllLoads(null, LoadStatus.POSTED, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllLoads_WithBothFilters_ShouldFilterResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Load> loadPage = new PageImpl<>(Collections.singletonList(testLoad));

        when(loadRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(loadPage);

        Page<LoadResponse> result = loadService.getAllLoads("shipper-123", LoadStatus.POSTED, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void cancelLoad_WhenLoadIsPosted_ShouldCancelSuccessfully() {
        testLoad.setStatus(LoadStatus.POSTED);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));
        when(loadRepository.save(any(Load.class))).thenReturn(testLoad);

        LoadResponse response = loadService.cancelLoad(loadId);

        assertEquals(LoadStatus.CANCELLED, testLoad.getStatus());
        verify(loadRepository).save(testLoad);
    }

    @Test
    void cancelLoad_WhenLoadIsAlreadyCancelled_ShouldThrowException() {
        testLoad.setStatus(LoadStatus.CANCELLED);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> loadService.cancelLoad(loadId));
        assertTrue(exception.getMessage().contains("already cancelled"));
    }

    @Test
    void cancelLoad_WhenLoadIsBooked_ShouldThrowException() {
        testLoad.setStatus(LoadStatus.BOOKED);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> loadService.cancelLoad(loadId));
        assertTrue(exception.getMessage().contains("Cannot cancel a booked load"));
    }

    @Test
    void updateLoadAfterBooking_WhenTrucksRemain_ShouldUpdateRemainingTrucks() {
        testLoad.setRemainingTrucks(10);
        when(loadRepository.save(any(Load.class))).thenReturn(testLoad);

        loadService.updateLoadAfterBooking(testLoad, 3);

        assertEquals(7, testLoad.getRemainingTrucks());
        assertEquals(LoadStatus.POSTED, testLoad.getStatus()); // Status unchanged
        verify(loadRepository).save(testLoad);
    }

    @Test
    void updateLoadAfterBooking_WhenNoTrucksRemain_ShouldMarkAsBooked() {
        testLoad.setRemainingTrucks(5);
        when(loadRepository.save(any(Load.class))).thenReturn(testLoad);

        loadService.updateLoadAfterBooking(testLoad, 5);

        assertEquals(0, testLoad.getRemainingTrucks());
        assertEquals(LoadStatus.BOOKED, testLoad.getStatus());
        verify(loadRepository).save(testLoad);
    }

    @Test
    void updateLoadAfterBooking_WhenBookingExceedsTrucks_ShouldSetToZeroAndBooked() {
        testLoad.setRemainingTrucks(3);
        when(loadRepository.save(any(Load.class))).thenReturn(testLoad);

        loadService.updateLoadAfterBooking(testLoad, 5);

        assertEquals(0, testLoad.getRemainingTrucks()); // Clamped to 0
        assertEquals(LoadStatus.BOOKED, testLoad.getStatus());
    }

    @Test
    void save_ShouldCallRepositorySave() {
        when(loadRepository.save(testLoad)).thenReturn(testLoad);

        loadService.save(testLoad);

        verify(loadRepository, times(1)).save(testLoad);
    }
}
