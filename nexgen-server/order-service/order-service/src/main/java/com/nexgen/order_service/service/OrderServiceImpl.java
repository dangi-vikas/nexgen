package com.nexgen.order_service.service;

import com.nexgen.order_service.dto.*;
import com.nexgen.order_service.entity.Order;
import com.nexgen.order_service.entity.OrderItem;
import com.nexgen.order_service.entity.OrderStatus;
import com.nexgen.order_service.entity.OrderStatusHistory;
import com.nexgen.order_service.exception.OrderNotFoundException;
import com.nexgen.order_service.repository.OrderRepository;
import com.nexgen.order_service.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final OrderKafkaProducerService kafkaProducerService;
    private final OrderStatusHistoryRepository historyRepository;

    @Caching(evict = {
        @CacheEvict(value = "orders", key = "#result.orderNumber", condition = "#result != null"),
        @CacheEvict(value = "ordersByUser", allEntries = true)
   })
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
                    orderItem.setOrder(order);
                    return orderItem;
                }).collect(Collectors.toList());
        order.setOrderItems(items);

        Order savedOrder = orderRepository.save(order);

        kafkaProducerService.sendOrderCreatedEvent(
            new OrderEvent(savedOrder.getOrderNumber(), savedOrder.getUserId(), savedOrder.getStatus(), Instant.now().toEpochMilli())
        );

        return mapToResponse(savedOrder);
    }

    @Cacheable(value = "orders", key = "#orderNumber")
    @Override
    public OrderResponse getOrderById(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        return mapToResponse(order);
    }

    @Cacheable(
            value = "ordersByUser",
            key = "#userId + ':' + #page + ':' + #size"
    )
    @Override
    public PagedOrderResponse getOrdersByUserId(String userId, int page, int size, OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage;

        if (status != null) {
            orderPage = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            orderPage = orderRepository.findByUserId(userId, pageable);
        }

        List<OrderResponse> content = orderPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PagedOrderResponse(
                content,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isLast()
        );
    }


    @Caching(evict = {
        @CacheEvict(value = "orders", key = "#orderNumber"),
        @CacheEvict(value = "ordersByUser", allEntries = true)
    })
    @Override
    public void cancelOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        order.setStatus(OrderStatus.CANCELLED);

        kafkaProducerService.sendOrderCancelledEvent(
                new OrderEvent(order.getOrderNumber(), order.getUserId(), order.getStatus(), Instant.now().toEpochMilli())
        );

        orderRepository.save(order);

    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "orders", key = "#orderNumber"),
            @CacheEvict(value = "ordersByUser", allEntries = true)
    })
    public void updateOrderStatus(String orderNumber, OrderStatus newStatus) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        OrderStatus currentStatus = order.getStatus();
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderNumber(orderNumber)
                .oldStatus(currentStatus)
                .newStatus(newStatus)
                .changedAt(Instant.now())
                .changedBy("system")
                .build();

        historyRepository.save(history);

        kafkaProducerService.sendOrderUpdatedEvent(
                new OrderEvent(order.getOrderNumber(), order.getUserId(), newStatus, Instant.now().toEpochMilli())
        );
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(item -> new OrderItemResponse(item.getSkuCode(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList());

        return new OrderResponse(order.getOrderNumber(), order.getUserId(), order.getStatus(), order.getCreatedAt(), items);
    }
}
