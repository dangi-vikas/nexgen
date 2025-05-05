package com.nexgen.cart_service.controller;

import com.nexgen.cart_service.dto.*;
import com.nexgen.cart_service.entity.CartItem;
import com.nexgen.cart_service.exception.InvalidQuantityException;
import com.nexgen.cart_service.service.CartEventProducerService;
import com.nexgen.cart_service.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart Controller", description = "Manage customer cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartEventProducerService cartEventProducer;

    @Operation(summary = "Get current items in the cart")
    @GetMapping("/{userId}")
    public ResponseEntity<List<CartItemResponse>> getCartByUser(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.getCartByUser(userId));
    }

    @Operation(summary = "Add an item to the cart")
    @PostMapping("/{userId}/add")
    public ResponseEntity<CartItemResponse> addItemToCart(
            @PathVariable String userId,
            @RequestBody CartItemRequest itemRequest
    ) {
        CartItemResponse item = cartService.addItemToCart(userId, itemRequest);

        cartEventProducer.sendAddToCartEvent(
                new AddToCartEvent(userId, item.getProductId(), item.getQuantity(), item.getPrice())
        );

        return ResponseEntity.ok(item);
    }

    @Operation(summary = "Remove items from the cart")
    @DeleteMapping("/{userId}/remove/{productId}/quantity/{quantity}")
    public ResponseEntity<CartItemResponse> removeItemQuantity(
            @PathVariable String userId,
            @PathVariable String productId,
            @PathVariable int quantity) {

        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity to remove must be greater than 0.");
        }

        CartItemResponse item = cartService.removeItemQuantity(userId, productId, quantity);

        cartEventProducer.sendRemoveFromCartEvent(
                new RemoveFromCartEvent(userId, productId, quantity, "Item removed from cart")
        );

        return ResponseEntity.ok(item);
    }

    @Operation(summary = "Clear the cart")
    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);

        List<CartItem> items = cartService.getItemsByUserId(userId);
        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();

        CartClearedEvent event = new CartClearedEvent(userId, totalQuantity, "Cart cleared successfully.");
        cartEventProducer.sendCartClearedEvent(event);

        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Pay and Checkout")
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest request) {
        CheckoutResponse response = cartService.checkout(request);
        List<CartItem> items = cartService.getItemsByUserId(request.getUserId());

        cartEventProducer.sendCheckoutEvent(
                new CheckoutEvent(
                        request.getUserId(),
                        items.stream().map(CartItem::getProductId).toList(),
                        response.getTotalAmount(),
                        "Checkout completed"
                )
        );

        return ResponseEntity.ok(response);
    }
}
