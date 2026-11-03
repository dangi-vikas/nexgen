package com.nexgen.cart_service.service;

import com.nexgen.cart_service.client.OrderServiceClientImpl;
import com.nexgen.cart_service.client.ProductCatalogClient;
import com.nexgen.cart_service.dto.*;
import com.nexgen.cart_service.entity.CartItem;
import com.nexgen.cart_service.exception.CartEmptyException;
import com.nexgen.cart_service.exception.CartItemNotFoundException;
import com.nexgen.cart_service.exception.InvalidQuantityException;
import com.nexgen.cart_service.repository.CartItemRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartItemRepository cartRepository;
    private final MeterRegistry meterRegistry;
    private final ProductCatalogClient productCatalogClient;
    private final OrderServiceClientImpl orderServiceClient;

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
    @CircuitBreaker(name = "cartServiceCircuitBreaker", fallbackMethod = "fallbackGetCartFallback")
    @RateLimiter(name = "cartServiceRateLimiter")
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
    @Timed(value = "cart.add.item", description = "Time taken to add item to cart")
    @Override
    public CartItemResponse addItemToCart(String userId, CartItemRequest itemRequest) {
        addToCartCounter.increment();

        if (itemRequest == null) {
            throw new InvalidQuantityException("Item request is required");
        }
        if (itemRequest.getProductId() == null || itemRequest.getProductId().isBlank()) {
            throw new InvalidQuantityException("Product ID is required");
        }
        if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
            throw new InvalidQuantityException("Quantity to add must be greater than 0.");
        }

        ProductCatalogClient.ProductSnapshot product = productCatalogClient.getProduct(itemRequest.getProductId());
        BigDecimal unitPrice = product.price();

        CartItem item;
        if (cartRepository.existsByUserIdAndProductId(userId, itemRequest.getProductId())) {
            item = cartRepository.findByUserIdAndProductId(userId, itemRequest.getProductId());
            if (item == null) {
                item = CartItem.builder()
                        .userId(userId)
                        .productId(itemRequest.getProductId())
                        .quantity(itemRequest.getQuantity())
                        .price(unitPrice)
                        .version(0L)
                        .build();
            } else {
                item.setQuantity(item.getQuantity() + itemRequest.getQuantity());
                item.setPrice(unitPrice);
            }
        } else {
            item = CartItem.builder()
                    .userId(userId)
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .price(unitPrice)
                    .version(0L)
                    .build();
        }

        CartItem saved = cartRepository.save(item);

        return mapToResponse(saved);
    }

    @CacheEvict(value = "cart", key = "#userId")
    @RateLimiter(name = "cartServiceRateLimiter")
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
        BigDecimal unitPrice = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();

        if (updatedQuantity > 0) {
            item.setQuantity(updatedQuantity);
            item.setPrice(unitPrice);
            CartItem updated = cartRepository.save(item);
            return mapToResponse(updated);
        } else {
            cartRepository.delete(item);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            return CartItemResponse.builder()
                    .userId(userId)
                    .productId(productId)
                    .quantity(0)
                    .price(unitPrice)
                    .lineTotal(lineTotal)
                    .build();
        }
    }

    @CacheEvict(value = "cart", key = "#userId")
    @CircuitBreaker(name = "cartServiceCircuitBreaker", fallbackMethod = "fallbackClearCart")
    @RateLimiter(name = "cartServiceRateLimiter")
    @Timed(value = "cart.clear", description = "Time taken to clear cart")
    @Override
    public void clearCart(String userId) {
        clearCartCounter.increment();

        List<CartItem> items = cartRepository.findByUserId(userId);

        if (items == null || items.isEmpty()) {
            throw new CartEmptyException("Cart is already empty for userId: " + userId);
        }

        cartRepository.deleteByUserId(userId);
    }

    @CacheEvict(value = "cart", key = "#request.userId")
    @CircuitBreaker(name = "cartServiceCircuitBreaker", fallbackMethod = "fallbackCheckout")
    @RateLimiter(name = "cartServiceRateLimiter")
    @Timed(value = "cart.checkout", description = "Time taken to checkout cart")
    @Override
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        return checkout(request, null);
    }

    @CacheEvict(value = "cart", key = "#request.userId")
    @CircuitBreaker(name = "cartServiceCircuitBreaker", fallbackMethod = "fallbackCheckout")
    @RateLimiter(name = "cartServiceRateLimiter")
    @Timed(value = "cart.checkout", description = "Time taken to checkout cart")
    @Override
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request, String authorizationHeader) {
        checkoutCounter.increment();

        if (request == null) {
            throw new CartEmptyException("Checkout request is required");
        }
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new CartEmptyException("User ID is required");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new CartEmptyException("Idempotency key is required");
        }

        List<CartItem> items = cartRepository.findByUserId(request.getUserId());

        if (items == null || items.isEmpty()) {
            throw new CartEmptyException("Cannot checkout an empty cart");
        }

        List<com.nexgen.cart_service.dto.CartItemRequest> orderItems = new ArrayList<>();
        for (CartItem item : items) {
            com.nexgen.cart_service.dto.CartItemRequest r = new com.nexgen.cart_service.dto.CartItemRequest(item.getProductId(), item.getQuantity());
            orderItems.add(r);
        }

        CheckoutRequest orderReq = new CheckoutRequest();
        orderReq.setUserId(request.getUserId());
        orderReq.setIdempotencyKey(request.getIdempotencyKey());
        orderReq.setPaymentMethod(request.getPaymentMethod());

        CheckoutResponse response = orderServiceClient.checkoutWithItems(orderReq, orderItems, authorizationHeader);

        cartRepository.deleteByUserId(request.getUserId());

        if (response == null) {
            throw new CartEmptyException("Checkout failed");
        }
        if (response.getPaymentStatus() == null) {
            try {
                java.lang.reflect.Field p = CheckoutResponse.class.getDeclaredField("paymentStatus");
                p.setAccessible(true);
                p.set(response, "NOT_IMPLEMENTED");
            } catch (Exception ignored) {
            }
        }
        try {
            java.lang.reflect.Field s = CheckoutResponse.class.getDeclaredField("status");
            if (s.get(response) == null) {
                java.lang.reflect.Field p2 = CheckoutResponse.class.getDeclaredField("paymentStatus");
                p2.setAccessible(true);
                Object ps = p2.get(response);
                s.setAccessible(true);
                s.set(response, ps == null ? "NOT_IMPLEMENTED" : ps);
            }
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field u = CheckoutResponse.class.getDeclaredField("userId");
            if (u.get(response) == null) {
                u.setAccessible(true);
                u.set(response, request.getUserId());
            }
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field pid = CheckoutResponse.class.getDeclaredField("purchasedProductIds");
            if (pid.get(response) == null) {
                pid.setAccessible(true);
                pid.set(response, items.stream().map(CartItem::getProductId).collect(Collectors.toList()));
            }
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field ta = CheckoutResponse.class.getDeclaredField("totalAmount");
            if (ta.get(response) == null) {
                BigDecimal total = BigDecimal.ZERO;
                for (CartItem item : items) {
                    BigDecimal unitPrice = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
                    total = total.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
                }
                ta.setAccessible(true);
                ta.set(response, total);
            }
        } catch (Exception ignored) {
        }

        return response;
    }

    private CartItemResponse mapToResponse(CartItem item) {
        BigDecimal unitPrice = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        return CartItemResponse.builder()
                .userId(item.getUserId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(unitPrice)
                .lineTotal(lineTotal)
                .build();
    }

    public List<CartItemResponse> fallbackGetCart(String userId, Throwable t) {
        return List.of();
    }

    public List<CartItemResponse> fallbackGetCartFallback(String userId, Throwable t) {
        log.error("Cart service fallback: getCartByUser failed", t);
        throw new RuntimeException("Cart service unavailable");
    }

    public CartItemResponse fallbackAddItem(String userId, CartItemRequest request, Throwable t) {
        log.error("Cart service fallback: addItemToCart failed", t);
        throw new RuntimeException("Cart service unavailable");
    }

    public CartItemResponse fallbackRemoveItem(String userId, String productId, int qty, Throwable t) {
        log.error("Cart service fallback: removeItemQuantity failed", t);
        throw new RuntimeException("Cart service unavailable");
    }

    public void fallbackClearCart(String userId, Throwable t) {
        log.error("Cart service fallback: clearCart failed", t);
        throw new RuntimeException("Cart service unavailable");
    }

    public CheckoutResponse fallbackCheckout(CheckoutRequest request, Throwable t) {
        log.error("Cart service fallback: checkout failed", t);
        throw new RuntimeException("Cart service unavailable");
    }

    public List<CartItem> getItemsByUserId(String userId) {
        return cartRepository.findByUserId(userId);
    }
}
