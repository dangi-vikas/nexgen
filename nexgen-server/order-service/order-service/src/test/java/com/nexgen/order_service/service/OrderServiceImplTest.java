package com.nexgen.order_service.service;

import com.nexgen.order_service.dto.*;
import com.nexgen.order_service.entity.Order;
import com.nexgen.order_service.entity.OrderItem;
import com.nexgen.order_service.entity.OrderStatus;
import com.nexgen.order_service.exception.OrderNotFoundException;
import com.nexgen.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderKafkaProducerService kafkaProducerService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateOrder() {
        OrderRequest request = new OrderRequest();
        request.setUserId("user123");
        request.setOrderItems(List.of(
                new OrderItemRequest("SKU1", 2, 100.0)
        ));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals("user123", response.getUserId());
        assertEquals(OrderStatus.CREATED, response.getStatus());
        assertEquals(1, response.getOrderItems().size());
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        verify(kafkaProducerService).sendOrderCreatedEvent(any(OrderEvent.class));
    }

    @Test
    void testGetOrderById_whenFound() {
        Order order = new Order();
        order.setOrderNumber("ORDER123");
        order.setUserId("user123");
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(Instant.now());
        order.setOrderItems(List.of(new OrderItem(1L,"SKU1", 2, 50.0, order)));

        when(orderRepository.findByOrderNumber("ORDER123")).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById("ORDER123");

        assertEquals("ORDER123", response.getOrderNumber());
        assertEquals("user123", response.getUserId());
        assertEquals(1, response.getOrderItems().size());
    }

    @Test
    void testGetOrderById_whenNotFound() {
        when(orderRepository.findByOrderNumber("ORDER123")).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById("ORDER123"));
    }

    @Test
    void testGetOrdersByUserId() {
        String userId = "user123";
        Pageable pageable = PageRequest.of(0, 2);

        Order order = new Order();
        order.setOrderNumber("ORDER123");
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(Instant.now());
        order.setOrderItems(List.of(new OrderItem(1L,"SKU1", 2, 50.0, order)));

        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);

        when(orderRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

        PagedOrderResponse response = orderService.getOrdersByUserId(userId, 0, 2);

        assertEquals(1, response.getContent().size());
        assertEquals("ORDER123", response.getContent().get(0).getOrderNumber());
    }

    @Test
    void testCancelOrder_whenFound() {
        Order order = new Order();
        order.setOrderNumber("ORDER123");
        order.setUserId("user123");
        order.setStatus(OrderStatus.CREATED);

        when(orderRepository.findByOrderNumber("ORDER123")).thenReturn(Optional.of(order));

        orderService.cancelOrder("ORDER123");

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(kafkaProducerService).sendOrderCancelledEvent(any(OrderEvent.class));
        verify(orderRepository).save(order);
    }

    @Test
    void testCancelOrder_whenNotFound() {
        when(orderRepository.findByOrderNumber("ORDER123")).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.cancelOrder("ORDER123"));
    }
}
