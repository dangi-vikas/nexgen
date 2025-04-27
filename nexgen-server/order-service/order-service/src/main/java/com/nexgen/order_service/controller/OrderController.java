package com.nexgen.order_service.controller;

import com.nexgen.order_service.dto.OrderRequest;
import com.nexgen.order_service.dto.OrderResponse;
import com.nexgen.order_service.dto.PagedOrderResponse;
import com.nexgen.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        OrderResponse orderResponse = orderService.createOrder(orderRequest);
        return ResponseEntity.ok(orderResponse);
    }

    @Operation(summary = "Get order by Order Number")
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByOrderNumber(@PathVariable String orderNumber) {
        OrderResponse orderResponse = orderService.getOrderById(orderNumber);
        return ResponseEntity.ok(orderResponse);
    }

    @Operation(summary = "Get all orders by User ID")
    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedOrderResponse> getOrdersByUserId(@PathVariable String userId,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        PagedOrderResponse pagedOrderResponse = orderService.getOrdersByUserId(userId, page, size);
        return ResponseEntity.ok(pagedOrderResponse);
    }

    @Operation(summary = "Cancel an order by Order Number")
    @PutMapping("/{orderNumber}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderNumber) {
        orderService.cancelOrder(orderNumber);
        return ResponseEntity.noContent().build();
    }
}
