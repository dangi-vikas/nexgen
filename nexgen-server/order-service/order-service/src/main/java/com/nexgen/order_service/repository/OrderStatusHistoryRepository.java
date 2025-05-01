package com.nexgen.order_service.repository;

import com.nexgen.order_service.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderNumberOrderByChangedAtDesc(String orderNumber);
}
