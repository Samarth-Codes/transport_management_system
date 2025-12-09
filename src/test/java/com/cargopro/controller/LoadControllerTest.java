package com.cargopro.controller;

import com.cargopro.dto.BestBidResponse;
import com.cargopro.dto.LoadRequest;
import com.cargopro.dto.LoadResponse;
import com.cargopro.enums.LoadStatus;
import com.cargopro.enums.WeightUnit;
import com.cargopro.exception.InvalidStatusTransitionException;
import com.cargopro.exception.ResourceNotFoundException;
import com.cargopro.service.BidService;
import com.cargopro.service.LoadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoadController.class)
class LoadControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private LoadService loadService;

        @MockBean
        private BidService bidService;

        private LoadResponse testLoadResponse;
        private UUID loadId;

        @BeforeEach
        void setUp() {
                loadId = UUID.randomUUID();
                testLoadResponse = new LoadResponse(
                                loadId,
                                "shipper-123",
                                "New York",
                                "Los Angeles",
                                new Timestamp(System.currentTimeMillis()),
                                "Electronics",
                                1000.0,
                                WeightUnit.KG,
                                "LARGE",
                                10,
                                10,
                                LoadStatus.POSTED,
                                new Timestamp(System.currentTimeMillis()));
        }

        @Test
        void createLoad_Success() throws Exception {
                LoadRequest request = new LoadRequest();
                request.setShipperId("shipper-123");
                request.setLoadingCity("New York");
                request.setUnloadingCity("Los Angeles");
                request.setProductType("Electronics");
                request.setTruckType("LARGE");
                request.setNoOfTrucks(10);
                request.setWeight(1000.0);
                request.setWeightUnit(WeightUnit.KG);

                when(loadService.createLoad(any(LoadRequest.class))).thenReturn(testLoadResponse);

                mockMvc.perform(post("/load")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.loadId").value(loadId.toString()))
                                .andExpect(jsonPath("$.loadingCity").value("New York"))
                                .andExpect(jsonPath("$.status").value("POSTED"));
        }

        @Test
        void createLoad_ValidationError_MissingShipperId() throws Exception {
                LoadRequest request = new LoadRequest();
                request.setLoadingCity("New York");
                request.setUnloadingCity("Los Angeles");
                request.setProductType("Electronics");
                request.setTruckType("LARGE");
                request.setNoOfTrucks(10);
                request.setWeight(1000.0);
                request.setWeightUnit(WeightUnit.KG);

                mockMvc.perform(post("/load")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.shipperId").value("Shipper ID is required"));
        }

        @Test
        void createLoad_ValidationError_InvalidNoOfTrucks() throws Exception {
                LoadRequest request = new LoadRequest();
                request.setShipperId("shipper-123");
                request.setLoadingCity("New York");
                request.setUnloadingCity("Los Angeles");
                request.setProductType("Electronics");
                request.setTruckType("LARGE");
                request.setNoOfTrucks(0);
                request.setWeight(1000.0);
                request.setWeightUnit(WeightUnit.KG);

                mockMvc.perform(post("/load")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.noOfTrucks").value("Number of trucks must be at least 1"));
        }

        @Test
        void getAllLoads_Success() throws Exception {
                Page<LoadResponse> page = new PageImpl<>(
                                Collections.singletonList(testLoadResponse),
                                PageRequest.of(0, 10),
                                1);
                when(loadService.getAllLoads(any(), any(), any(Pageable.class))).thenReturn(page);

                mockMvc.perform(get("/load"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(1)))
                                .andExpect(jsonPath("$.content[0].loadId").value(loadId.toString()));
        }

        @Test
        void getAllLoads_WithFilters() throws Exception {
                Page<LoadResponse> page = new PageImpl<>(
                                Collections.singletonList(testLoadResponse),
                                PageRequest.of(0, 10),
                                1);
                when(loadService.getAllLoads(any(), any(), any(Pageable.class))).thenReturn(page);

                mockMvc.perform(get("/load")
                                .param("shipperId", "shipper-123")
                                .param("status", "POSTED")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        void getLoadById_Success() throws Exception {
                when(loadService.getLoadById(loadId)).thenReturn(testLoadResponse);

                mockMvc.perform(get("/load/{id}", loadId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.loadId").value(loadId.toString()))
                                .andExpect(jsonPath("$.loadingCity").value("New York"));
        }

        @Test
        void getLoadById_NotFound() throws Exception {
                when(loadService.getLoadById(loadId))
                                .thenThrow(new ResourceNotFoundException("Load not found with id: " + loadId));

                mockMvc.perform(get("/load/{id}", loadId))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value(containsString("Load not found")));
        }

        @Test
        void cancelLoad_Success() throws Exception {
                LoadResponse cancelledResponse = new LoadResponse(
                                loadId, "shipper-123", "New York", "Los Angeles",
                                new Timestamp(System.currentTimeMillis()), "Electronics",
                                1000.0, WeightUnit.KG, "LARGE", 10, 10,
                                LoadStatus.CANCELLED, new Timestamp(System.currentTimeMillis()));
                when(loadService.cancelLoad(loadId)).thenReturn(cancelledResponse);

                mockMvc.perform(patch("/load/{id}/cancel", loadId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        void cancelLoad_AlreadyCancelled() throws Exception {
                when(loadService.cancelLoad(loadId))
                                .thenThrow(new InvalidStatusTransitionException("Load is already cancelled"));

                mockMvc.perform(patch("/load/{id}/cancel", loadId))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Load is already cancelled"));
        }

        @Test
        void cancelLoad_NotFound() throws Exception {
                when(loadService.cancelLoad(loadId))
                                .thenThrow(new ResourceNotFoundException("Load not found with id: " + loadId));

                mockMvc.perform(patch("/load/{id}/cancel", loadId))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value(containsString("Load not found")));
        }

        @Test
        void getBestBids_Success() throws Exception {
                BestBidResponse bid1 = new BestBidResponse(
                                UUID.randomUUID(), UUID.randomUUID(), "Company A", 4.5,
                                500.0, 5, 0.8, new Timestamp(System.currentTimeMillis()));
                BestBidResponse bid2 = new BestBidResponse(
                                UUID.randomUUID(), UUID.randomUUID(), "Company B", 4.0,
                                600.0, 5, 0.6, new Timestamp(System.currentTimeMillis()));
                when(bidService.getBestBidsForLoad(loadId)).thenReturn(Arrays.asList(bid1, bid2));

                mockMvc.perform(get("/load/{id}/best-bids", loadId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].transporterName").value("Company A"))
                                .andExpect(jsonPath("$[1].transporterName").value("Company B"));
        }

        @Test
        void getBestBids_EmptyList() throws Exception {
                when(bidService.getBestBidsForLoad(loadId)).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/load/{id}/best-bids", loadId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));
        }
}
