package com.example.bidmartbooking.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingOrderControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateOrderAndGetItById() throws Exception {
        String createPayload = """
            {
              "itemName": "Keyboard",
              "quantity": 2,
              "notes": "Please wrap safely"
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.itemName").value("Keyboard"))
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();

        JsonNode createJson = objectMapper.readTree(
            createResult.getResponse().getContentAsString()
        );
        long orderId = createJson.get("id").asLong();

        mockMvc.perform(get("/api/orders/{orderId}", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.itemName").value("Keyboard"))
            .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void shouldUpdateOrderStatus() throws Exception {
        String createPayload = """
            {
              "itemName": "Monitor",
              "quantity": 1
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();

        JsonNode createJson = objectMapper.readTree(
            createResult.getResponse().getContentAsString()
        );
        long orderId = createJson.get("id").asLong();

        String updatePayload = """
            {
              "status": "CONFIRMED"
            }
            """;

        mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldListOrders() throws Exception {
        String createPayload = """
            {
              "itemName": "Mouse",
              "quantity": 3
            }
            """;

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].itemName").exists())
            .andExpect(jsonPath("$[0].status").exists());
    }
}
