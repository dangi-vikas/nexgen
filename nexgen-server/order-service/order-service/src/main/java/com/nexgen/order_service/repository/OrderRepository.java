package com.nexgen.order_service.repository;

import com.nexgen.order_service.entity.Order;
import com.nexgen.order_service.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(String userId, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable);
}
