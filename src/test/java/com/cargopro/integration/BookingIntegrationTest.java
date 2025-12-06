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
public class BookingIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        private String loadId;
        private String transporterId;
        private String bidId;

        @BeforeEach
        public void setup() throws Exception {
                // 1. Create Load
                LoadRequest loadReq = new LoadRequest();
                loadReq.setLoadingCity("X");
                loadReq.setUnloadingCity("Y");
                loadReq.setShipperId("shipper-booking");
                loadReq.setProductType("Misc");
                loadReq.setTruckType(TruckType.LARGE.name());
                loadReq.setNoOfTrucks(10);
                loadReq.setWeight(100.0);
                loadReq.setWeightUnit(WeightUnit.KG);

                String loadJson = mockMvc.perform(post("/load")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loadReq)))
                                .andReturn().getResponse().getContentAsString();
                loadId = com.jayway.jsonpath.JsonPath.read(loadJson, "$.loadId");

                // 2. Create Transporter
                TransporterRequest transReq = new TransporterRequest();
                transReq.setCompanyName("Booker Inc");
                transReq.setRating(5.0);
                transReq.setAvailableTrucks(Arrays.asList(
                                new TruckAvailability(TruckType.LARGE.name(), 20)));

                String transJson = mockMvc.perform(post("/transporter")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(transReq)))
                                .andReturn().getResponse().getContentAsString();
                transporterId = com.jayway.jsonpath.JsonPath.read(transJson, "$.transporterId");

                // 3. Create Bid
                BidRequest bidReq = new BidRequest();
                bidReq.setLoadId(UUID.fromString(loadId));
                bidReq.setTransporterId(UUID.fromString(transporterId));
                bidReq.setProposedRate(100.0);
                bidReq.setTrucksOffered(5);

                String bidJson = mockMvc.perform(post("/bid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(bidReq)))
                                .andReturn().getResponse().getContentAsString();
                bidId = com.jayway.jsonpath.JsonPath.read(bidJson, "$.bidId");
        }

        @Test
        public void shouldAcceptBidAndCreateBooking() throws Exception {
                // Accept Bid
                mockMvc.perform(post("/booking?bidId=" + bidId))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.bookingId").exists());

                // Verify Load remaining trucks: 10 - 5 = 5
                mockMvc.perform(get("/load/" + loadId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.remainingTrucks", is(5)));

                // Verify Transporter available trucks logic (need to verify list content)
                // This is slightly tricky with list, but we can check if response has list.
                // Or we can query transporter endpoints.
                // Assuming Transporter response includes availableTrucks.
                // We'll skip complex list verification for now and trust the booking response
                // or load logic.
                // Actually we can check Load remaining trucks easily.
        }

        @Test
        public void shouldAutoMarkLoadAsBooked() throws Exception {
                // Create another bid for remaining 5 trucks
                BidRequest bidReq2 = new BidRequest();
                bidReq2.setLoadId(UUID.fromString(loadId));
                bidReq2.setTransporterId(UUID.fromString(transporterId));
                bidReq2.setProposedRate(100.0);
                bidReq2.setTrucksOffered(5);

                String bidJson2 = mockMvc.perform(post("/bid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(bidReq2)))
                                .andReturn().getResponse().getContentAsString();
                String bidId2 = com.jayway.jsonpath.JsonPath.read(bidJson2, "$.bidId");

                // Accept first bid (-5 trucks)
                mockMvc.perform(post("/booking?bidId=" + bidId));

                // Accept second bid (-5 trucks, total 0)
                mockMvc.perform(post("/booking?bidId=" + bidId2))
                                .andExpect(status().isCreated());

                // Verify Load status is BOOKED
                mockMvc.perform(get("/load/" + loadId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", is("BOOKED")))
                                .andExpect(jsonPath("$.remainingTrucks", is(0)));
        }
}
