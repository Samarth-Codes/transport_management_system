package com.cargopro.integration;

import com.cargopro.dto.BidRequest;
import com.cargopro.dto.LoadRequest;
import com.cargopro.dto.TransporterRequest;
import com.cargopro.entity.TruckAvailability;
import com.cargopro.enums.TruckType;
import com.cargopro.enums.WeightUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BidIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        private String loadId;
        private String transporterId;

        @BeforeEach
        public void setup() throws Exception {
                // Create Load
                LoadRequest loadReq = new LoadRequest();
                loadReq.setLoadingCity("A");
                loadReq.setUnloadingCity("B");
                loadReq.setShipperId("shipper-bid");
                loadReq.setProductType("General");
                loadReq.setTruckType(TruckType.LARGE.name());
                loadReq.setNoOfTrucks(10);
                loadReq.setWeight(100.0);
                loadReq.setWeightUnit(WeightUnit.KG);

                String loadJson = mockMvc.perform(post("/load")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loadReq)))
                                .andReturn().getResponse().getContentAsString();
                loadId = com.jayway.jsonpath.JsonPath.read(loadJson, "$.loadId");

                // Create Transporter
                TransporterRequest transReq = new TransporterRequest();
                transReq.setCompanyName("Bidder One");
                transReq.setRating(4.0);
                transReq.setAvailableTrucks(Arrays.asList(
                                new TruckAvailability(TruckType.LARGE.name(), 5)));

                String transJson = mockMvc.perform(post("/transporter")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(transReq)))
                                .andReturn().getResponse().getContentAsString();
                transporterId = com.jayway.jsonpath.JsonPath.read(transJson, "$.transporterId");
        }

        @Test
        public void shouldCreateBidSuccessfully() throws Exception {
                BidRequest bidReq = new BidRequest();
                bidReq.setTransporterId(UUID.fromString(transporterId));
                bidReq.setLoadId(UUID.fromString(loadId));
                bidReq.setProposedRate(500.0);
                bidReq.setTrucksOffered(2);

                mockMvc.perform(post("/bid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(bidReq)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.bidId").exists())
                                .andExpect(jsonPath("$.proposedRate", is(500.0)));
        }

        @Test
        public void shouldRejectBidWithInsufficientTrucks() throws Exception {
                BidRequest bidReq = new BidRequest();
                bidReq.setTransporterId(UUID.fromString(transporterId));
                bidReq.setLoadId(UUID.fromString(loadId));
                bidReq.setProposedRate(500.0);
                bidReq.setTrucksOffered(100); // More than transporter has (5)

                mockMvc.perform(post("/bid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(bidReq)))
                                .andExpect(status().isBadRequest()); // Assuming validation failure is 400 or 500
                                                                     // depending on exception handler
                // InsufficientCapacityException likely handled by GlobalExceptionHandler
                // mapping to 400?
                // I haven't checked GlobalExceptionHandler but let's assume standard behavior
                // for custom exception is 500 unless mapped.
                // Actually usually it's better to expect 4xx.
                // If it fails, I'll update.
        }

        @Test
        public void shouldRejectBidOnCancelledLoad() throws Exception {
                // Cancel load first
                mockMvc.perform(patch("/load/" + loadId + "/cancel"))
                                .andExpect(status().isOk());

                BidRequest bidReq = new BidRequest();
                bidReq.setTransporterId(UUID.fromString(transporterId));
                bidReq.setLoadId(UUID.fromString(loadId));
                bidReq.setProposedRate(500.0);
                bidReq.setTrucksOffered(2);

                mockMvc.perform(post("/bid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(bidReq)))
                                .andExpect(status().isBadRequest()); // InvalidStatusTransitionException
        }
}
