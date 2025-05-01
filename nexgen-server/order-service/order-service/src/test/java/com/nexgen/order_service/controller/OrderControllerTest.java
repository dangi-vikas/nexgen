package com.nexgen.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.order_service.dto.*;
import com.nexgen.order_service.entity.OrderStatus;
import com.nexgen.order_service.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("POST /api/v1/orders - Create Order")
    void shouldCreateOrder() throws Exception {
        OrderRequest orderRequest = new OrderRequest("user123", List.of(
                new OrderItemRequest("ITEM001", 2, 200.00)
        ));

        OrderResponse orderResponse = new OrderResponse(
                "ORD001", "user123", OrderStatus.CREATED, Instant.now(),
                List.of(new OrderItemResponse("ITEM001", 2, 200.00))
        );

        Mockito.when(orderService.createOrder(any(OrderRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD001"))
                .andExpect(jsonPath("$.userId").value("user123"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderNumber} - Get Order by Order Number")
    void shouldReturnOrderByOrderNumber() throws Exception {
        OrderResponse orderResponse = new OrderResponse(
                "ORD002", "user456", OrderStatus.CREATED, Instant.now(),
                List.of(new OrderItemResponse("ITEM002", 1, 120.00))
        );

        Mockito.when(orderService.getOrderById("ORD002")).thenReturn(orderResponse);

        mockMvc.perform(get("/api/v1/orders/ORD002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD002"))
                .andExpect(jsonPath("$.userId").value("user456"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/user/{userId} - Get Orders by User ID")
    void shouldReturnPagedOrdersByUserId() throws Exception {
        List<OrderResponse> orders = List.of(
                new OrderResponse("ORD003", "user789", OrderStatus.CREATED, Instant.now(),
                        List.of(new OrderItemResponse("ITEM003", 3, 300.00)))
        );

        PagedOrderResponse pagedOrderResponse = new PagedOrderResponse(
                orders, 0, 10, 1, 1, true
        );

        Mockito.when(orderService.getOrdersByUserId("user789", 0, 10)).thenReturn(pagedOrderResponse);

        mockMvc.perform(get("/api/v1/orders/user/user789?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD003"))
                .andExpect(jsonPath("$.content[0].userId").value("user789"));
    }

    @Test
    @DisplayName("PUT /api/v1/orders/{orderNumber}/cancel - Cancel Order")
    void shouldCancelOrder() throws Exception {
        Mockito.doNothing().when(orderService).cancelOrder("ORD004");

        mockMvc.perform(put("/api/v1/orders/ORD004/cancel"))
                .andExpect(status().isNoContent());
    }
}
