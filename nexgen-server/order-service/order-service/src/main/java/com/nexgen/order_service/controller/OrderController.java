package com.nexgen.order_service.controller;

import com.nexgen.order_service.dto.OrderRequest;
import com.nexgen.order_service.dto.OrderResponse;
import com.nexgen.order_service.dto.PagedOrderResponse;
import com.nexgen.order_service.entity.OrderStatus;
import com.nexgen.order_service.entity.OrderStatusHistory;
import com.nexgen.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Controller", description = "Manage customer orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create a new order")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest orderRequest, Authentication authentication) {
        // Stock is reserved asynchronously by the inventory service consuming
        // order-created; the response is therefore CREATED, not CONFIRMED.
        OrderResponse orderResponse = orderService.createOrder(orderRequest, authentication.getName());
        return ResponseEntity.ok(orderResponse);
    }

    @Operation(summary = "Get order by Order Number")
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByOrderNumber(
            @PathVariable String orderNumber, Authentication authentication) {
        OrderResponse orderResponse = orderService.getOrderById(
                orderNumber, authentication.getName(), isPrivileged(authentication));
        return ResponseEntity.ok(orderResponse);
    }

    @Operation(summary = "Get all orders by User ID")
    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedOrderResponse> getOrdersByUserId(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status,
            Authentication authentication) {

        if (!isPrivileged(authentication)) {
            requireOwner(userId, authentication);
        }

        PagedOrderResponse pagedOrderResponse = orderService.getOrdersByUserId(userId, page, size, status);
        return ResponseEntity.ok(pagedOrderResponse);
    }

    @Operation(summary = "Cancel an order by Order Number")
    @PutMapping("/{orderNumber}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderNumber, Authentication authentication) {
        orderService.cancelOrder(orderNumber, authentication.getName(), isPrivileged(authentication));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{orderNumber}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable String orderNumber,
            @RequestParam OrderStatus status,
            Authentication authentication) {
        requirePrivileged(authentication);
        orderService.updateOrderStatus(orderNumber, status);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{orderNumber}/history")
    public ResponseEntity<List<OrderStatusHistory>> getOrderHistory(
            @PathVariable String orderNumber, Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrderHistory(
                orderNumber, authentication.getName(), isPrivileged(authentication)));
    }

    private boolean isPrivileged(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority) || "ROLE_INTERNAL".equals(authority));
    }

    private void requirePrivileged(Authentication authentication) {
        if (!isPrivileged(authentication)) {
            throw new AccessDeniedException("Admin or internal role is required");
        }
    }

    private void requireOwner(String userId, Authentication authentication) {
        if (authentication == null || !userId.equals(authentication.getName())) {
            throw new AccessDeniedException("You may only access your own orders");
        }
    }
}
