package com.nexgen.cart_service.repository;

import com.nexgen.cart_service.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(String userId);
    void deleteByUserId(String userId);
    boolean existsByUserIdAndProductId(String userId, String productId);
    CartItem findByUserIdAndProductId(String userId, String productId);

}
