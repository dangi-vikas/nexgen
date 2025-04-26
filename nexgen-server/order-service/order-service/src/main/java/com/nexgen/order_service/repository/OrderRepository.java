package com.nexgen.order_service.repository;

import com.nexgen.order_service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(String userId);
    Optional<Order> findByOrderNumber(String orderNumber);
}
