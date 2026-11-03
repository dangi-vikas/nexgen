package com.nexgen.cart_service.service;

import com.nexgen.cart_service.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional cart reactions used by the Kafka listeners.
 *
 * <p>The listeners are deliberately not transactional: they call one of these
 * methods (which commits), and only then record the event as processed. Marking it
 * inside the same listener transaction would let the marker commit while the cart
 * change rolled back, losing the event forever.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CartReactionService {

    private final CartItemRepository cartRepository;

    @Transactional
    public void clearCartForUser(String userId) {
        cartRepository.deleteByUserId(userId);
        log.info("Cleared cart of user {}", userId);
    }

    @Transactional
    public void removeProductFromAllCarts(String productId) {
        cartRepository.deleteByProductId(productId);
        log.info("Removed product {} from all carts", productId);
    }
}
