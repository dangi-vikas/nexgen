package com.nexgen.order_service.service;

import com.nexgen.order_service.dto.*;
import com.nexgen.order_service.entity.Order;
import com.nexgen.order_service.entity.OrderItem;
import com.nexgen.order_service.entity.OrderStatus;
import com.nexgen.order_service.exception.OrderNotFoundException;
import com.nexgen.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    public OrderResponse createOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setUserId(orderRequest.getUserId());
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(Instant.now());

        List<OrderItem> items = orderRequest.getOrderItems()
                .stream()
                .map(item -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setSkuCode(item.getSkuCode());
                    orderItem.setQuantity(item.getQuantity());
                    orderItem.setPrice(item.getPrice());
                    return orderItem;
                }).collect(Collectors.toList());
        order.setOrderItems(items);

        Order savedOrder = orderRepository.save(order);

        return mapToResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrderById(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        return mapToResponse(order);
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void cancelOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(item -> new OrderItemResponse(item.getSkuCode(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList());

        return new OrderResponse(order.getOrderNumber(), order.getUserId(), order.getStatus(), order.getCreatedAt(), items);
    }
}
