package com.nexgen.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.order_service.config.SecurityConfig;
import com.nexgen.order_service.dto.*;
import com.nexgen.order_service.entity.OrderStatus;
import com.nexgen.order_service.entity.OrderStatusHistory;
import com.nexgen.order_service.service.OrderService;
import com.nexgen.order_service.support.JwtTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=01234567890123456789012345678901",
        "jwt.issuer=nexgen-user-service",
        "jwt.audience=nexgen-api"
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("POST /api/v1/orders - Create Order")
    void shouldCreateOrder() throws Exception {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOrderItems(List.of(new OrderItemRequest("ITEM001", 2)));
        orderRequest.setIdempotencyKey("key1");

        OrderResponse orderResponse = new OrderResponse(
                "ORD001", "user123", OrderStatus.CREATED, Instant.now(), java.math.BigDecimal.valueOf(400.00),
                List.of(new OrderItemResponse("ITEM001", 2, java.math.BigDecimal.valueOf(200.00), java.math.BigDecimal.valueOf(400.00)))
        );

        when(orderService.createOrder(any(OrderRequest.class), eq("user123"))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().jwt(jwt -> jwt.subject("user123"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
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
                "ORD002", "user456", OrderStatus.CREATED, Instant.now(), java.math.BigDecimal.valueOf(120.00),
                List.of(new OrderItemResponse("ITEM002", 1, java.math.BigDecimal.valueOf(120.00), java.math.BigDecimal.valueOf(120.00)))
        );

        when(orderService.getOrderById("ORD002", "user456", false)).thenReturn(orderResponse);

        mockMvc.perform(get("/api/v1/orders/ORD002")
                        .with(jwt().jwt(jwt -> jwt.subject("user456"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD002"))
                .andExpect(jsonPath("$.userId").value("user456"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/user/{userId} - Get Orders by User ID")
    void shouldReturnPagedOrdersByUserId() throws Exception {
        List<OrderResponse> orders = List.of(
                new OrderResponse("ORD003", "user789", OrderStatus.CREATED, Instant.now(), java.math.BigDecimal.valueOf(900.00),
                        List.of(new OrderItemResponse("ITEM003", 3, java.math.BigDecimal.valueOf(300.00), java.math.BigDecimal.valueOf(900.00))))
        );

        PagedOrderResponse pagedOrderResponse = new PagedOrderResponse(
                orders, 0, 10, 1, 1, true
        );

        when(orderService.getOrdersByUserId("user789", 0, 10, null)).thenReturn(pagedOrderResponse);

        mockMvc.perform(get("/api/v1/orders/user/user789?page=0&size=10")
                        .with(jwt().jwt(jwt -> jwt.subject("user789"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD003"))
                .andExpect(jsonPath("$.content[0].userId").value("user789"));
    }

    @Test
    void shouldGetOrdersByUserIdWithoutStatus() throws Exception {
        when(orderService.getOrdersByUserId(anyString(), anyInt(), anyInt(), isNull()))
                .thenReturn(new PagedOrderResponse(List.of(),  0, 0, 0, 10, false));

        mockMvc.perform(get("/api/v1/orders/user/123")
                        .with(jwt().jwt(jwt -> jwt.subject("123"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldGetOrdersByUserIdWithStatus() throws Exception {
        when(orderService.getOrdersByUserId(anyString(), anyInt(), anyInt(), eq(OrderStatus.CONFIRMED)))
                .thenReturn(new PagedOrderResponse(List.of(), 0, 0, 0, 10, false));

        mockMvc.perform(get("/api/v1/orders/user/123")
                        .with(jwt().jwt(jwt -> jwt.subject("123"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldCancelOrder() throws Exception {
        doNothing().when(orderService).cancelOrder(anyString(), anyString(), anyBoolean());

        mockMvc.perform(put("/api/v1/orders/ORD123/cancel")
                        .with(jwt().jwt(jwt -> jwt.subject("user123"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldUpdateOrderStatus() throws Exception {
        doNothing().when(orderService).updateOrderStatus(anyString(), any(OrderStatus.class));

        mockMvc.perform(put("/api/v1/orders/ORD123/status")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("status", "DELIVERED"))
                .andExpect(status().isNoContent());
    }

    @Test
    void createOrderWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItems\":[{\"skuCode\":\"ITEM001\",\"quantity\":1,\"price\":10.0}]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrdersForAnotherUserIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/orders/user/victim")
                        .with(jwt().jwt(jwt -> jwt.subject("attacker"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateOrderStatusRequiresAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/orders/ORD123/status")
                        .with(jwt().jwt(jwt -> jwt.subject("user123"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("status", "DELIVERED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrderHistoryForAnotherUserIsForbiddenAtServiceBoundary() throws Exception {
        when(orderService.getOrderHistory("ORD123", "attacker", false))
                .thenThrow(new com.nexgen.order_service.exception.OrderNotFoundException("ORD123"));

        mockMvc.perform(get("/api/v1/orders/ORD123/history")
                        .with(jwt().jwt(jwt -> jwt.subject("attacker"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetOrderHistory() throws Exception {
        List<OrderStatusHistory> historyList = List.of(
                OrderStatusHistory.builder()
                        .id(1L)
                        .orderNumber("ORD123")
                        .oldStatus(OrderStatus.CONFIRMED)
                        .newStatus(OrderStatus.DELIVERED)
                        .changedAt(Instant.now())
                        .changedBy("system")
                        .build()
        );

        when(orderService.getOrderHistory("ORD123", "user123", false)).thenReturn(historyList);

        mockMvc.perform(get("/api/v1/orders/ORD123/history")
                        .with(jwt().jwt(jwt -> jwt.subject("user123"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("ORD123"))
                .andExpect(jsonPath("$[0].newStatus").value("DELIVERED"));
    }

    @Test
    void realAccessTokenIsAcceptedByResourceServer() throws Exception {
        OrderResponse orderResponse = new OrderResponse(
                "ORD002", "user456", OrderStatus.CREATED, Instant.now(), java.math.BigDecimal.valueOf(120.00),
                List.of(new OrderItemResponse("ITEM002", 1, java.math.BigDecimal.valueOf(120.00), java.math.BigDecimal.valueOf(120.00)))
        );
        when(orderService.getOrderById("ORD002", "user456", false)).thenReturn(orderResponse);

        mockMvc.perform(get("/api/v1/orders/ORD002")
                        .header("Authorization", "Bearer " + JwtTestSupport.accessToken("user456", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user456"));
    }

    @Test
    void refreshTokenIsRejectedByResourceServer() throws Exception {
        mockMvc.perform(get("/api/v1/orders/ORD002")
                        .header("Authorization", "Bearer " + JwtTestSupport.refreshToken("user456")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithWrongAudienceIsRejected() throws Exception {
        String token = JwtTestSupport.token("nexgen-user-service", "some-other-api",
                "access", "user456", "ROLE_USER");

        mockMvc.perform(get("/api/v1/orders/ORD002")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithWrongIssuerIsRejected() throws Exception {
        String token = JwtTestSupport.token("rogue-issuer", "nexgen-api",
                "access", "user456", "ROLE_USER");

        mockMvc.perform(get("/api/v1/orders/ORD002")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void realUserTokenCannotUpdateOrderStatus() throws Exception {
        mockMvc.perform(put("/api/v1/orders/ORD123/status")
                        .header("Authorization", "Bearer " + JwtTestSupport.accessToken("user123", "ROLE_USER"))
                        .param("status", "DELIVERED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void realAdminTokenCanUpdateOrderStatus() throws Exception {
        doNothing().when(orderService).updateOrderStatus(anyString(), any(OrderStatus.class));

        mockMvc.perform(put("/api/v1/orders/ORD123/status")
                        .header("Authorization", "Bearer " + JwtTestSupport.accessToken("admin", "ROLE_ADMIN"))
                        .param("status", "DELIVERED"))
                .andExpect(status().isNoContent());
    }
}
