package com.cargopro.integration;

import com.cargopro.dto.LoadRequest;
import com.cargopro.enums.TruckType;
import com.cargopro.enums.WeightUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Load Management
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LoadIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        public void shouldCreateLoadSuccessfully() throws Exception {
                LoadRequest request = new LoadRequest();
                request.setLoadingCity("New York");
                request.setUnloadingCity("Los Angeles");
                request.setShipperId("shipper-123");
                request.setProductType("Electronics");
                request.setTruckType(TruckType.LARGE.name());
                request.setNoOfTrucks(10);
                request.setWeight(1000.0);
                request.setWeightUnit(WeightUnit.KG);

                mockMvc.perform(post("/load")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.loadId").exists())
                                .andExpect(jsonPath("$.loadingCity", is("New York")))
                                .andExpect(jsonPath("$.loadingDate").exists())
                                .andExpect(jsonPath("$.status", is("POSTED")))
                                .andExpect(jsonPath("$.remainingTrucks", is(10)));
        }

        @Test
        public void shouldGetLoadById() throws Exception {
                LoadRequest request = new LoadRequest();
                request.setLoadingCity("Chicago");
                request.setUnloadingCity("Houston");
                request.setShipperId("shipper-456");
                request.setProductType("Furniture");
                request.setTruckType(TruckType.MEDIUM.name());
                request.setNoOfTrucks(5);
                request.setWeight(500.0);
                request.setWeightUnit(WeightUnit.KG);

                String responseJson = mockMvc.perform(post("/load")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();

                String id = com.jayway.jsonpath.JsonPath.read(responseJson, "$.loadId");

                mockMvc.perform(get("/load/" + id))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.loadingCity", is("Chicago")))
                                .andExpect(jsonPath("$.noOfTrucks", is(5)));
        }

        @Test
        public void shouldCancelLoad() throws Exception {
                LoadRequest request = new LoadRequest();
                request.setLoadingCity("Miami");
                request.setUnloadingCity("Seattle");
                request.setShipperId("shipper-789");
                request.setProductType("Food");
                request.setTruckType(TruckType.SMALL.name());
                request.setNoOfTrucks(2);
                request.setWeight(200.0);
                request.setWeightUnit(WeightUnit.KG);

                String responseJson = mockMvc.perform(post("/load")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andReturn().getResponse().getContentAsString();

                String id = com.jayway.jsonpath.JsonPath.read(responseJson, "$.loadId");

                mockMvc.perform(patch("/load/" + id + "/cancel"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", is("CANCELLED")));
        }
}
