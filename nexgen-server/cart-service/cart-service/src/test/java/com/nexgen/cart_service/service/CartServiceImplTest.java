package com.nexgen.cart_service.service;

import com.nexgen.cart_service.client.OrderServiceClientImpl;
import com.nexgen.cart_service.client.ProductCatalogClient;
import com.nexgen.cart_service.dto.*;
import com.nexgen.cart_service.entity.CartItem;
import com.nexgen.cart_service.exception.CartEmptyException;
import com.nexgen.cart_service.exception.InvalidQuantityException;
import com.nexgen.cart_service.repository.CartItemRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartItemRepository cartRepository;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private ProductCatalogClient productCatalogClient;
    @Mock
    private OrderServiceClientImpl orderServiceClient;

    @Mock
    private CartEventProducerService cartEventProducer;

    @Mock
    private Counter getCartItemCounter;
    @Mock
    private Counter addToCartCounter;
    @Mock
    private Counter removeFromCartCounter;
    @Mock
    private Counter clearCartCounter;
    @Mock
    private Counter checkoutCounter;

    @InjectMocks
    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter("cart.get.items.count")).thenReturn(getCartItemCounter);
        when(meterRegistry.counter("cart.add.count")).thenReturn(addToCartCounter);
        when(meterRegistry.counter("cart.remove.count")).thenReturn(removeFromCartCounter);
        when(meterRegistry.counter("cart.clear.count")).thenReturn(clearCartCounter);
        when(meterRegistry.counter("cart.checkout.count")).thenReturn(checkoutCounter);

        cartService.initCounters();
    }

    @Test
    void testGetCartByUser_shouldReturnCartItems() {
        String userId = "user123";
        CartItem item = CartItem.builder()
                .userId(userId)
                .productId("prod1")
                .quantity(2)
                .price(BigDecimal.valueOf(200.0))
                .build();

        when(cartRepository.findByUserId(userId)).thenReturn(List.of(item));

        List<CartItemResponse> response = cartService.getCartByUser(userId);

        assertEquals(1, response.size());
        assertEquals("prod1", response.get(0).getProductId());
        verify(getCartItemCounter).increment();
    }

    @Test
    void testAddItemToCart_newItem_shouldSaveAndReturn() {
        String userId = "user123";
        CartItemRequest request = new CartItemRequest("prod1", 2);

        when(cartRepository.existsByUserIdAndProductId(userId, "prod1")).thenReturn(false);
        when(productCatalogClient.getProduct("prod1")).thenReturn(new ProductCatalogClient.ProductSnapshot("prod1", "p", BigDecimal.valueOf(100.0)));

        CartItem saved = CartItem.builder()
                .userId(userId)
                .productId("prod1")
                .quantity(2)
                .price(BigDecimal.valueOf(100.0))
                .build();

        when(cartRepository.save(any(CartItem.class))).thenReturn(saved);

        CartItemResponse response = cartService.addItemToCart(userId, request);

        assertEquals("prod1", response.getProductId());
        verify(addToCartCounter).increment();
    }

    @Test
    void testRemoveItemQuantity_reduceToZero_shouldDeleteItem() {
        String userId = "user123";
        String productId = "prod1";
        CartItem item = CartItem.builder()
                .userId(userId)
                .productId(productId)
                .quantity(2)
                .price(BigDecimal.valueOf(100.0))
                .build();

        when(cartRepository.findByUserIdAndProductId(userId, productId)).thenReturn(item);

        CartItemResponse response = cartService.removeItemQuantity(userId, productId, 2);

        assertEquals(0, response.getQuantity());
        verify(removeFromCartCounter).increment();
        verify(cartRepository).delete(item);
    }

    @Test
    void testClearCart_shouldDeleteByUserId() {
        String userId = "user123";
        CartItem item1 = CartItem.builder().userId(userId).productId("prod1").quantity(2).price(BigDecimal.valueOf(200.0)).build();
        CartItem item2 = CartItem.builder().userId(userId).productId("prod2").quantity(3).price(BigDecimal.valueOf(300.0)).build();

        when(cartRepository.findByUserId(userId)).thenReturn(List.of(item1, item2));

        cartService.clearCart(userId);

        verify(clearCartCounter).increment();
        verify(cartRepository).deleteByUserId(userId);
    }

    @Test
    void testCheckout_shouldReturnCheckoutResponse() {
        String userId = "user123";
        CheckoutRequest request = new CheckoutRequest(userId, "CREDIT CARD", "idem1");

        CartItem item1 = CartItem.builder().userId(userId).productId("prod1").quantity(2).price(BigDecimal.valueOf(200.0)).build();
        CartItem item2 = CartItem.builder().userId(userId).productId("prod2").quantity(3).price(BigDecimal.valueOf(300.0)).build();

        when(cartRepository.findByUserId(userId)).thenReturn(List.of(item1, item2));
        CheckoutResponse cr = new CheckoutResponse();
        cr.setUserId(userId);
        cr.setOrderNumber("ORD1");
        cr.setStatus("CREATED");
        cr.setPaymentStatus("NOT_IMPLEMENTED");
        cr.setTotalAmount(BigDecimal.valueOf(500.0));
        cr.setPurchasedProductIds(List.of("prod1", "prod2"));
        when(orderServiceClient.checkoutWithItems(any(CheckoutRequest.class), anyList(), any())).thenReturn(cr);

        CheckoutResponse response = cartService.checkout(request);

        assertEquals(BigDecimal.valueOf(500.0), response.getTotalAmount());
        assertEquals("NOT_IMPLEMENTED", response.getPaymentStatus());
        // The cart survives checkout: order-confirmed clears it, so a rejected
        // reservation does not silently empty the customer's cart.
        verify(cartRepository, never()).deleteByUserId(userId);
        verify(cartEventProducer).sendCheckoutEvent(any(CheckoutEvent.class));
        verify(checkoutCounter).increment();
    }

    @Test
    void testAddItemToCart_withNegativeQuantity_shouldThrowException() {
        String userId = "user123";
        CartItemRequest request = new CartItemRequest("prod1", -2);

        InvalidQuantityException exception = assertThrows(
                InvalidQuantityException.class,
                () -> cartService.addItemToCart(userId, request)
        );

        assertEquals("Quantity to add must be greater than 0.", exception.getMessage());
    }

    @Test
    void testRemoveItemQuantity_itemNotFound_shouldThrowException() {
        String userId = "user123";
        String productId = "prod1";

        when(cartRepository.findByUserIdAndProductId(userId, productId)).thenReturn(null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cartService.removeItemQuantity(userId, productId, 1)
        );

        assertEquals("Cart item not found for userId: user123 and productId: prod1", exception.getMessage());
    }

    @Test
    void testCheckout_emptyCart_shouldThrowException() {
        String userId = "user123";
        CheckoutRequest request = new CheckoutRequest(userId, "CREDIT CARD", "idem1");

        when(cartRepository.findByUserId(userId)).thenReturn(List.of());

        CartEmptyException exception = assertThrows(
                CartEmptyException.class,
                () -> cartService.checkout(request)
        );

        assertEquals("Cannot checkout an empty cart", exception.getMessage());
    }

}
