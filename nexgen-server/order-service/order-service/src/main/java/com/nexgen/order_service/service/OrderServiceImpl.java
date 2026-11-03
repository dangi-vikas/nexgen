package com.nexgen.order_service.service;

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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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

/**
 * Order aggregate.
 *
 * <p>Order creation is a local transaction plus a single {@code order-created}
 * event in the outbox: stock is reserved by the inventory service consuming that
 * event, never by a synchronous HTTP call. The order therefore starts as CREATED
 * and is moved to CONFIRMED or CANCELLED by {@code inventory-reserved} /
 * {@code inventory-rejected}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String CORRELATION_ID = "correlationId";

    private final OrderRepository orderRepository;
    private final OrderKafkaProducerService kafkaProducerService;
    private final OrderStatusHistoryRepository historyRepository;
    private final MeterRegistry meterRegistry;
    private final ProductCatalogClient productCatalogClient;

    @CacheEvict(value = "ordersByUser", allEntries = true)
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest, String userId) {
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
                throw new InvalidOrderException("Quantity must be at least 1");
            }
            if (itemReq.getSkuCode() == null || itemReq.getSkuCode().isBlank()) {
                throw new InvalidOrderException("SKU code is required");
            }
            ProductCatalogClient.ProductSnapshot product = productCatalogClient.getProduct(itemReq.getSkuCode());
            BigDecimal unitPrice = product.price();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
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

        Order savedOrder;
        try {
            savedOrder = orderRepository.save(order);
        } catch (DataIntegrityViolationException ex) {
            // Two concurrent requests with the same idempotency key: return the winner.
            var replay = orderRepository.findByUserIdAndIdempotencyKey(userId, orderRequest.getIdempotencyKey());
            if (replay.isPresent()) {
                return mapToResponse(replay.get());
            }
            throw ex;
        }

        meterRegistry.counter("order.created", "order", savedOrder.getOrderNumber()).increment();

        kafkaProducerService.sendOrderCreatedEvent(buildOrderCreatedEvent(savedOrder));

        return mapToResponse(savedOrder);
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
        String reason = "Order cancelled by " + (requesterId == null ? "system" : requesterId);
        transition(order, OrderStatus.CANCELLED, requesterId == null ? "system" : requesterId, reason);
    }

    @Override
    @CacheEvict(value = "ordersByUser", allEntries = true)
    @Transactional
    public void updateOrderStatus(String orderNumber, OrderStatus newStatus) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        meterRegistry.counter("order.status.updated", "status", newStatus.name()).increment();

        transition(order, newStatus, "system", null);
    }

    @Override
    @CacheEvict(value = "ordersByUser", allEntries = true)
    @Transactional
    public void confirmAfterInventoryReservation(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        if (order.getStatus() != OrderStatus.CREATED) {
            log.info("Order {} is already {}, ignoring inventory-reserved", orderNumber, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        saveHistory(order, OrderStatus.CREATED, OrderStatus.CONFIRMED, "inventory-service");

        kafkaProducerService.sendOrderConfirmedEvent(OrderEvent.builder()
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(OrderStatus.CONFIRMED)
                .timestamp(Instant.now().toEpochMilli())
                .correlationId(MDC.get(CORRELATION_ID))
                .build());
    }

    @Override
    @CacheEvict(value = "ordersByUser", allEntries = true)
    @Transactional
    public void cancelAfterInventoryRejection(String orderNumber, String reason) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        if (order.getStatus() != OrderStatus.CREATED) {
            log.info("Order {} is already {}, ignoring inventory-rejected", orderNumber, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        saveHistory(order, OrderStatus.CREATED, OrderStatus.CANCELLED, "inventory-service");

        kafkaProducerService.sendOrderCancelledEvent(OrderEvent.builder()
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(OrderStatus.CANCELLED)
                .timestamp(Instant.now().toEpochMilli())
                .correlationId(MDC.get(CORRELATION_ID))
                .reason(reason == null ? "Inventory reservation rejected" : reason)
                .build());
    }

    @Override
    public List<OrderStatusHistory> getOrderHistory(String orderNumber, String requesterId, boolean privileged) {
        findOrderForRequester(orderNumber, requesterId, privileged);
        return historyRepository.findByOrderNumberOrderByChangedAtDesc(orderNumber);
    }

    private void transition(Order order, OrderStatus newStatus, String changedBy, String reason) {
        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == newStatus) {
            return;
        }
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
        saveHistory(order, currentStatus, newStatus, changedBy);

        OrderEvent event = OrderEvent.builder()
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(newStatus)
                .timestamp(Instant.now().toEpochMilli())
                .correlationId(MDC.get(CORRELATION_ID))
                .reason(reason)
                .build();

        if (newStatus == OrderStatus.CANCELLED) {
            // Inventory has to release whatever it reserved for this order.
            kafkaProducerService.sendOrderCancelledEvent(event);
        } else {
            kafkaProducerService.sendOrderUpdatedEvent(event);
        }
    }

    private void saveHistory(Order order, OrderStatus oldStatus, OrderStatus newStatus, String changedBy) {
        historyRepository.save(OrderStatusHistory.builder()
                .orderNumber(order.getOrderNumber())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedAt(Instant.now())
                .changedBy(changedBy)
                .build());
    }

    private OrderCreatedEvent buildOrderCreatedEvent(Order savedOrder) {
        List<OrderCreatedEvent.OrderItemEvent> itemEvents = savedOrder.getOrderItems() == null ? List.of()
                : savedOrder.getOrderItems().stream()
                .map(i -> {
                    BigDecimal lineTotal = i.getLineTotal();
                    if (lineTotal == null && i.getPrice() != null && i.getQuantity() != null) {
                        lineTotal = i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
                    }
                    return OrderCreatedEvent.OrderItemEvent.builder()
                            .skuCode(i.getSkuCode())
                            .quantity(i.getQuantity())
                            .price(i.getPrice())
                            .lineTotal(lineTotal)
                            .build();
                })
                .collect(Collectors.toList());

        return OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderNumber(savedOrder.getOrderNumber())
                .userId(savedOrder.getUserId())
                .status(savedOrder.getStatus())
                .totalAmount(savedOrder.getTotalAmount())
                .items(itemEvents)
                .timestamp(Instant.now())
                .correlationId(MDC.get(CORRELATION_ID))
                .idempotencyKey(savedOrder.getIdempotencyKey())
                .build();
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
