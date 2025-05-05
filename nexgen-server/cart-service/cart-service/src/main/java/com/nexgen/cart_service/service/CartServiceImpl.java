package com.nexgen.cart_service.service;

import com.nexgen.cart_service.dto.*;
import com.nexgen.cart_service.entity.CartItem;
import com.nexgen.cart_service.exception.CartEmptyException;
import com.nexgen.cart_service.exception.CartItemNotFoundException;
import com.nexgen.cart_service.exception.InvalidQuantityException;
import com.nexgen.cart_service.repository.CartItemRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartRepository;
    private final CartEventProducerService cartEventProducer;
    private final MeterRegistry meterRegistry;

    private Counter getCartItemCounter;
    private Counter addToCartCounter;
    private Counter removeFromCartCounter;
    private Counter clearCartCounter;
    private Counter checkoutCounter;

    @PostConstruct
    public void initCounters() {
        this.getCartItemCounter = meterRegistry.counter("cart.get.items.count");
        this.addToCartCounter = meterRegistry.counter("cart.add.count");
        this.removeFromCartCounter = meterRegistry.counter("cart.remove.count");
        this.clearCartCounter = meterRegistry.counter("cart.clear.count");
        this.checkoutCounter = meterRegistry.counter("cart.checkout.count");
    }

    @Cacheable(value = "cart", key = "#userId")
    @CircuitBreaker(name = "cartServiceCircuitBreaker", fallbackMethod = "fallbackGetCart")
    @RateLimiter(name = "cartServiceRateLimiter")
    @Retry(name = "cartServiceRetry", fallbackMethod = "fallbackForGetCart")
    @Timed(value = "cart.get.by.user", description = "Time taken to get cart by user")
    @Override
    public List<CartItemResponse> getCartByUser(String userId) {
        getCartItemCounter.increment();

        return cartRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "cart", key = "#userId")
    @CircuitBreaker(name = "cartServiceCircuitBreaker", fallbackMethod = "fallbackAddItem")
    @RateLimiter(name = "cartServiceRateLimiter")
    @Retry(name = "cartServiceRetry", fallbackMethod = "fallbackAddItem")
    @Timed(value = "cart.add.item", description = "Time taken to add item to cart")
    @Override
    public CartItemResponse addItemToCart(String userId, CartItemRequest itemRequest) {
        addToCartCounter.increment();

        CartItem item;

        if (itemRequest.getQuantity() <= 0) {
            throw new InvalidQuantityException("Quantity to add must be greater than 0.");
        }

        if (cartRepository.existsByUserIdAndProductId(userId, itemRequest.getProductId())) {
            item = cartRepository.findByUserIdAndProductId(userId, itemRequest.getProductId());
            item.setQuantity(item.getQuantity() + itemRequest.getQuantity());
            item.setPrice(item.getPrice() + (itemRequest.getPrice() * itemRequest.getQuantity()));
        } else {
            item = CartItem.builder()
                    .userId(userId)
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .price(itemRequest.getPrice() * itemRequest.getQuantity())
                    .build();
        }

        CartItem saved = cartRepository.save(item);

        try {
            cartEventProducer.sendAddToCartEvent(
                    new AddToCartEvent(userId, item.getProductId(), item.getQuantity(), item.getPrice())
            );
        } catch (Exception e) {
            log.error("Failed to publish Kafka AddToCartEvent for userId={} and productId={}", userId, item.getProductId(), e);
        }

        return mapToResponse(saved);
    }

    @CacheEvict(value = "cart", key = "#userId")
    @RateLimiter(name = "cartServiceRateLimiter")
    @Retry(name = "cartServiceRetry", fallbackMethod = "fallbackRemoveItem")
    @Timed(value = "cart.remove.item", description = "Time taken to remove item from cart")
    @Override
    public CartItemResponse removeItemQuantity(String userId, String productId, int quantity) {
        removeFromCartCounter.increment();

        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity to remove must be greater than 0.");
        }

        CartItem item = cartRepository.findByUserIdAndProductId(userId, productId);

        if (item == null) {
            throw new CartItemNotFoundException("Cart item not found for userId: " + userId + " and productId: " + productId);
        }

        if (item.getQuantity() < quantity) {
            throw new InvalidQuantityException("Reduce amount is greater than quantity in cart.");
        }

        int updatedQuantity = item.getQuantity() - quantity;
        double updatedPrice = item.getPrice() - (quantity * (item.getPrice() / item.getQuantity()));

        if (updatedQuantity > 0) {
            item.setQuantity(updatedQuantity);
            item.setPrice(updatedPrice);
            CartItem updated = cartRepository.save(item);
            cartEventProducer.sendRemoveFromCartEvent(
                    new RemoveFromCartEvent(userId, productId, quantity, "Item removed from cart")
            );
            return mapToResponse(updated);
        } else {
            cartRepository.delete(item);
            cartEventProducer.sendRemoveFromCartEvent(
                    new RemoveFromCartEvent(userId, productId, quantity, "Item removed from cart")
            );
            return CartItemResponse.builder()
                    .userId(userId)
                    .productId(productId)
                    .quantity(0)
                    .price(updatedPrice)
                    .build();
        }
    }

    @CacheEvict(value = "cart", key = "#userId")
    @CircuitBreaker(name = "cartServiceCircuitBreaker", fallbackMethod = "fallbackClearCart")
    @RateLimiter(name = "cartServiceRateLimiter")
    @Retry(name = "cartServiceRetry", fallbackMethod = "fallbackClearCart")
    @Timed(value = "cart.clear", description = "Time taken to clear cart")
    @Override
    public void clearCart(String userId) {
        clearCartCounter.increment();

        List<CartItem> items = cartRepository.findByUserId(userId);

        if (items.isEmpty()) {
            throw new CartEmptyException("Cart is already empty for userId: " + userId);
        }

        cartRepository.deleteByUserId(userId);

        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();

        CartClearedEvent event = new CartClearedEvent(userId, totalQuantity, "Cart cleared successfully.");
        cartEventProducer.sendCartClearedEvent(event);
    }

    @CacheEvict(value = "cart", key = "#request.userId")
    @CircuitBreaker(name = "cartServiceCircuitBreaker", fallbackMethod = "fallbackCheckout")
    @RateLimiter(name = "cartServiceRateLimiter")
    @Retry(name = "cartServiceRetry", fallbackMethod = "fallbackCheckout")
    @Timed(value = "cart.checkout", description = "Time taken to checkout cart")
    @Override
    public CheckoutResponse checkout(CheckoutRequest request) {
        checkoutCounter.increment();

        List<CartItem> items = cartRepository.findByUserId(request.getUserId());

        if (items.isEmpty()) {
            throw new CartEmptyException("Cannot checkout an empty cart");
        }

        double totalAmount = items.stream()
                .mapToDouble(CartItem::getPrice)
                .sum();

        List<String> purchasedProductIds = items.stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());

        cartRepository.deleteAll(items);

        CheckoutResponse response = CheckoutResponse.builder()
                .userId(request.getUserId())
                .paymentStatus("SUCCESS") // Simulated for now
                .totalAmount(totalAmount)
                .purchasedProductIds(purchasedProductIds)
                .build();

        cartEventProducer.sendCheckoutEvent(
                new CheckoutEvent(
                        request.getUserId(),
                        items.stream().map(CartItem::getProductId).toList(),
                        response.getTotalAmount(),
                        "Checkout completed"
                )
        );

        return response;
    }

    private CartItemResponse mapToResponse(CartItem item) {
        return CartItemResponse.builder()
                .userId(item.getUserId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }

    public List<CartItemResponse> fallbackGetCart(String userId, Throwable t) {
        return List.of();
    }

    public CartItemResponse fallbackAddItem(String userId, CartItemRequest request, Throwable t) {
        return CartItemResponse.builder()
                .userId(userId)
                .productId(request.getProductId())
                .quantity(0)
                .price(0)
                .build();
    }

    public CartItemResponse fallbackRemoveItem(String userId, String productId, int qty, Throwable t) {
        return CartItemResponse.builder()
                .userId(userId)
                .productId(productId)
                .quantity(0)
                .price(0)
                .build();
    }

    public void fallbackClearCart(String userId, Throwable t) {
        log.error("Some error occurred during clearing of the cart. Please try again.");
    }

    public CheckoutResponse fallbackCheckout(CheckoutRequest request, Throwable t) {
        return CheckoutResponse.builder()
                .userId(request.getUserId())
                .paymentStatus("FAILED")
                .totalAmount(0)
                .purchasedProductIds(List.of())
                .build();
    }
}
