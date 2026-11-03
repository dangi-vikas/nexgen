package com.nexgen.order_service.service;

import com.nexgen.order_service.client.InventoryClient;
import com.nexgen.order_service.client.ProductCatalogClient;
import com.nexgen.order_service.dto.*;
import com.nexgen.order_service.entity.Order;
import com.nexgen.order_service.entity.OrderItem;
import com.nexgen.order_service.entity.OrderStatus;
import com.nexgen.order_service.entity.OrderStatusHistory;
import com.nexgen.order_service.exception.InvalidOrderException;
import com.nexgen.order_service.exception.OrderNotFoundException;
import com.nexgen.order_service.repository.OrderRepository;
import com.nexgen.order_service.repository.OrderStatusHistoryRepository;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderKafkaProducerService kafkaProducerService;
    private final OrderStatusHistoryRepository historyRepository;
    private final MeterRegistry meterRegistry;
    private final ProductCatalogClient productCatalogClient;
    private final InventoryClient inventoryClient;
    private final com.nexgen.order_service.service.OutboxService outboxService;

    @CacheEvict(value = "ordersByUser", allEntries = true)
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest, String userId) {
        return createOrder(orderRequest, userId, null);
    }

    @CacheEvict(value = "ordersByUser", allEntries = true)
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest, String userId, String authorizationHeader) {
        if (orderRequest == null) {
            throw new InvalidOrderException("Order request is required");
        }
        if (userId == null || userId.isBlank()) {
            throw new InvalidOrderException("User is required");
        }
        if (orderRequest.getIdempotencyKey() == null || orderRequest.getIdempotencyKey().isBlank()) {
            throw new InvalidOrderException("Idempotency key is required");
        }
        if (orderRequest.getOrderItems() == null || orderRequest.getOrderItems().isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }

        var existing = orderRepository.findByUserIdAndIdempotencyKey(userId, orderRequest.getIdempotencyKey());
        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        String orderNumber = UUID.randomUUID().toString();
        String auth = authorizationHeader;
        if (auth != null && !auth.isBlank() && !auth.toLowerCase().startsWith("bearer")) {
            auth = "Bearer " + auth;
        }

        try {
            inventoryClient.reserve(orderNumber, orderRequest.getOrderItems(), auth);
        } catch (RuntimeException ex) {
            throw ex;
        }

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(Instant.now());
        order.setIdempotencyKey(orderRequest.getIdempotencyKey());
        order.setVersion(0L);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : orderRequest.getOrderItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                inventoryClient.release(orderNumber, auth);
                throw new InvalidOrderException("Quantity must be at least 1");
            }
            if (itemReq.getSkuCode() == null || itemReq.getSkuCode().isBlank()) {
                inventoryClient.release(orderNumber, auth);
                throw new InvalidOrderException("SKU code is required");
            }
            ProductCatalogClient.ProductSnapshot product = productCatalogClient.getProduct(itemReq.getSkuCode());
            BigDecimal unitPrice = product.price();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                inventoryClient.release(orderNumber, auth);
                throw new InvalidOrderException("Invalid product price for SKU: " + itemReq.getSkuCode());
            }
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            OrderItem orderItem = OrderItem.builder()
                    .skuCode(itemReq.getSkuCode())
                    .quantity(itemReq.getQuantity())
                    .price(unitPrice)
                    .lineTotal(lineTotal)
                    .order(order)
                    .build();
            items.add(orderItem);
            total = total.add(lineTotal);
        }
        order.setOrderItems(items);
        order.setTotalAmount(total);

        meterRegistry.counter("order.created", "order", order.getOrderNumber()).increment();

        try {
            Order savedOrder = orderRepository.save(order);
            try {
                kafkaProducerService.sendOrderCreatedEvent(
                    new OrderEvent(savedOrder.getOrderNumber(), savedOrder.getUserId(), savedOrder.getStatus(), Instant.now().toEpochMilli())
                );
                List<com.nexgen.order_service.dto.OrderCreatedEvent.OrderItemEvent> itemEvents = savedOrder.getOrderItems() == null ? List.of() : savedOrder.getOrderItems().stream()
                        .map(i -> {
                            BigDecimal lineTotal = i.getLineTotal();
                            if (lineTotal == null && i.getPrice() != null && i.getQuantity() != null) {
                                lineTotal = i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
                            }
                            return new com.nexgen.order_service.dto.OrderCreatedEvent.OrderItemEvent(i.getSkuCode(), i.getQuantity(), i.getPrice(), lineTotal);
                        })
                        .collect(java.util.stream.Collectors.toList());
                com.nexgen.order_service.dto.OrderCreatedEvent createdEvent = new com.nexgen.order_service.dto.OrderCreatedEvent(
                        savedOrder.getOrderNumber(),
                        savedOrder.getUserId(),
                        savedOrder.getStatus(),
                        savedOrder.getTotalAmount(),
                        itemEvents,
                        Instant.now(),
                        null,
                        savedOrder.getIdempotencyKey()
                );
                try {
                    if (outboxService != null) {
                        outboxService.enqueue("ORDER", savedOrder.getOrderNumber(), "order-created", "1.0", "order-service", null, null, createdEvent);
                    }
                } catch (Exception ignored) {
                }
                kafkaProducerService.sendOrderCreatedEvent(createdEvent);
            } catch (Exception ex) {
                // Don't fail order creation if event publishing fails
            }
            return mapToResponse(savedOrder);
        } catch (DataIntegrityViolationException ex) {
            inventoryClient.release(orderNumber, auth);
            String replayKey = null;
            if (orderRequest != null) {
                try { replayKey = orderRequest.getIdempotencyKey(); } catch (Exception ignored) {}
            }
            var replay = orderRepository.findByUserIdAndIdempotencyKey(userId, replayKey);
            if (replay.isPresent()) {
                return mapToResponse(replay.get());
            }
            throw ex;
        } catch (RuntimeException ex) {
            inventoryClient.release(orderNumber, auth);
            throw ex;
        }
    }

    @Timed(value = "order.get.by.id", description = "Time taken to fetch orders by orderId")
    @Override
    public OrderResponse getOrderById(String orderNumber, String requesterId, boolean privileged) {
        return mapToResponse(findOrderForRequester(orderNumber, requesterId, privileged));
    }

    @Timed(value = "order.get.by.userid", description = "Time taken to fetch orders by user")
    @Cacheable(
            value = "ordersByUser",
            key = "#userId + ':' + #page + ':' + #size + ':' + (#status == null ? 'ALL' : #status.name())"
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

        PagedOrderResponse paged = new PagedOrderResponse();
        paged.setContent(content);
        paged.setPageNumber(orderPage.getNumber());
        paged.setPageSize(orderPage.getSize());
        paged.setTotalElements(orderPage.getTotalElements());
        paged.setTotalPages(orderPage.getTotalPages());
        paged.setLast(orderPage.isLast());
        return paged;
    }

    @CacheEvict(value = "ordersByUser", allEntries = true)
    @Override
    @Transactional
    public void cancelOrder(String orderNumber, String requesterId, boolean privileged) {
        Order order = findOrderForRequester(orderNumber, requesterId, privileged);

        OrderStatus currentStatus = order.getStatus();
        if (!currentStatus.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new IllegalStateException("Invalid status transition from " + currentStatus + " to CANCELLED");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderNumber(orderNumber)
                .oldStatus(currentStatus)
                .newStatus(OrderStatus.CANCELLED)
                .changedAt(Instant.now())
                .changedBy(requesterId == null ? "system" : requesterId)
                .build();
        historyRepository.save(history);

        try {
            OrderEvent ev = new OrderEvent(order.getOrderNumber(), order.getUserId(), order.getStatus(), Instant.now().toEpochMilli());
            if (outboxService != null) {
                outboxService.enqueue("ORDER", order.getOrderNumber(), "order-cancelled", "1.0", "order-service", null, null, ev);
            }
            kafkaProducerService.sendOrderCancelledEvent(ev);
        } catch (Exception ignored) {
        }
    }

    @Override
    @CacheEvict(value = "ordersByUser", allEntries = true)
    @Transactional
    public void updateOrderStatus(String orderNumber, OrderStatus newStatus) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        meterRegistry.counter("order.status.updated", "status", newStatus.name()).increment();

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

        try {
            OrderEvent ev = new OrderEvent(order.getOrderNumber(), order.getUserId(), newStatus, Instant.now().toEpochMilli());
            if (outboxService != null) {
                outboxService.enqueue("ORDER", order.getOrderNumber(), "order-status-updated", "1.0", "order-service", null, null, ev);
            }
            kafkaProducerService.sendOrderUpdatedEvent(ev);
        } catch (Exception ignored) {
        }
    }

    @Override
    public List<OrderStatusHistory> getOrderHistory(String orderNumber, String requesterId, boolean privileged) {
        findOrderForRequester(orderNumber, requesterId, privileged);
        return historyRepository.findByOrderNumberOrderByChangedAtDesc(orderNumber);
    }

    private Order findOrderForRequester(String orderNumber, String requesterId, boolean privileged) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        if (!privileged && (requesterId == null || !requesterId.equals(order.getUserId()))) {
            // Do not reveal whether an order owned by another user exists.
            throw new OrderNotFoundException(orderNumber);
        }

        return order;
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems() == null ? List.of() : order.getOrderItems()
                .stream()
                .map(item -> {
                    BigDecimal lineTotal = item.getLineTotal();
                    if (lineTotal == null && item.getPrice() != null && item.getQuantity() != null) {
                        lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    }
                    return new OrderItemResponse(item.getSkuCode(), item.getQuantity(), item.getPrice(), lineTotal);
                })
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getOrderNumber(),
                order.getUserId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getTotalAmount(),
                items
        );
    }
}
